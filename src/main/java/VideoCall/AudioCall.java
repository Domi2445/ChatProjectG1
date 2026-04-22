package VideoCall;

import javax.sound.sampled.*;

public class AudioCall {
    private volatile boolean running = false;

    // relayIp   = Server IP (e.g., 127.0.0.1)
    // relayPort = 9000
    // myPort    = Our local port for receiving data (e.g., 7000)
	public int start(String relayIp, int relayPort, String roomId) throws Exception {
		running = true;

		UDPSender sender = new UDPSender(relayIp, relayPort);
		UDPReciever receiver = new UDPReciever(0); // OS wählt freien Port
		int myPort = receiver.getPort();

		// Room registry
		sender.sendString("JOIN:" + roomId);


		new Thread(() -> {
			TargetDataLine microphone = null;
			try {
				AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
				microphone = (TargetDataLine) AudioSystem.getLine(
					new DataLine.Info(TargetDataLine.class, format));
				microphone.open(format);
				microphone.start();

				byte[] buffer = new byte[1024];
				while (running) {
					int bytesRead = microphone.read(buffer, 0, buffer.length);
					byte[] data = new byte[bytesRead];
					System.arraycopy(buffer, 0, data, 0, bytesRead);
					sender.send(data);
				}
			} catch (Exception e) {
				e.printStackTrace();
				running = false; // stop receiver too if mic fails
			} finally {
				if (microphone != null) { microphone.stop(); microphone.close(); }
				sender.close();
				receiver.close(); // close receiver when sender fails
			}
		}).start();

        // Thread 2: Relay → Speakers
		new Thread(() -> {
			SourceDataLine speakers = null;
			try {
				AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
				speakers = (SourceDataLine) AudioSystem.getLine(
					new DataLine.Info(SourceDataLine.class, format));
				speakers.open(format);
				speakers.start();

				while (running) {
					byte[] data = receiver.receiver();
					speakers.write(data, 0, data.length);
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			} finally {
				if (speakers != null) { speakers.stop(); speakers.close(); }
				receiver.close();
			}
		}).start();

		return myPort;
	}

    public void stop() {
        running = false;
    }
}
