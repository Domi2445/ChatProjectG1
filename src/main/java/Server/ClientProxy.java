package Server;

import User.Model.User;
import Util.Network.Packet;
import Util.Network.SocketProxy;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientProxy implements AutoCloseable {
	/// Der Socket, über den mit diesem Client kommuniziert wird.
	private final SocketProxy socket;
	/// Queue für empfangene Pakete von diesem Client.
	private final BlockingQueue<Packet> inPacketQueue;
	/// Queue für zu sendende Pakete an diesen Client.
	private final BlockingQueue<Packet> outPacketQueue;
	/// Flag, um anzuzeigen, dass die Verbindung zu diesem Client geschlossen werden soll.
	private final AtomicBoolean stopFlag = new AtomicBoolean(false);

	private final Future<?> inPacketListenerFuture;
	private final Future<?> outPacketSenderFuture;

	/// Das zugehörige `User`-Objekt, falls der Client sich bereits angemeldet hat. Ansonsten `null`.
	private User user;

	public ClientProxy(SocketProxy socket, BlockingQueue<Packet> inPacketQueue, BlockingQueue<Packet> outPacketQueue, ExecutorService threadExecutor) {
		this.socket = socket;
		this.inPacketQueue = inPacketQueue;
		this.outPacketQueue = outPacketQueue;
		this.user = null;

		inPacketListenerFuture = threadExecutor.submit(inPacketListener());
		outPacketSenderFuture = threadExecutor.submit(outPacketSender());
	}

	private Runnable inPacketListener() {
		return () -> {
			while (!Thread.currentThread().isInterrupted()) {
				if (stopFlag.get()) break;
				try {
					Packet packet = (Packet) socket.getInputStream().readObject();
					inPacketQueue.put(packet);
				} catch (IOException e) {
					if (!stopFlag.get()) {
						System.err.println("Fehler beim Lesen:\n" + e);
					}
					stopFlag.set(true);
					break;
				} catch (ClassNotFoundException e) {
					System.err.println("Ungültiges Paket empfangen:\n" + e);
					stopFlag.set(true);
					break;
				} catch (InterruptedException e) {
					stopFlag.set(true);
					Thread.currentThread().interrupt();
					break;
				}
			}
		};
	}

	private Runnable outPacketSender() {
		return () -> {
			int maxQueuedPackets = 0;

			while (!Thread.currentThread().isInterrupted()) {
				if (stopFlag.get()) break;
				try {
					Packet packet = outPacketQueue.take();
					socket.getOutputStream().writeObject(packet);
					if (outPacketQueue.isEmpty() || maxQueuedPackets++ >= 8) {
						socket.getOutputStream().flush();
						maxQueuedPackets = 0;
					}
				} catch (IOException e) {
					if (!stopFlag.get()) {
						System.err.println("Fehler beim Senden:\n" + e);
					}
					stopFlag.set(true);
					break;
				} catch (InterruptedException e) {
					stopFlag.set(true);
					Thread.currentThread().interrupt();
					break;
				}
			}
		};
	}

	/// Versucht, ein Paket in die `outPacketQueue` einzufügen. Gibt `false` zurück, wenn die Queue bereits voll ist.
	public boolean tryEnqueuePacket(Packet packet) {
		return outPacketQueue.offer(packet);
	}

	public boolean shouldStop() {
		return stopFlag.get() || socket.isClosed();
	}

	@Override
	public void close() throws IOException {
		stopFlag.set(true);

		if (!inPacketListenerFuture.isDone()) {
			inPacketListenerFuture.cancel(true);
		}
		if (!outPacketSenderFuture.isDone()) {
			outPacketSenderFuture.cancel(true);
		}
		if (!socket.isClosed()) {
			socket.close();
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
