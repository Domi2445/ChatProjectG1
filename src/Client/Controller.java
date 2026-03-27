package Client;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Controller implements Runnable {
	private final BlockingQueue<String> outgoingMessageQueue;
	private final BlockingQueue<String> incomingMessageQueue;

	private View view;
	private final Client client;

	public Controller() throws IOException {
		this.outgoingMessageQueue = new ArrayBlockingQueue<>(4);
		this.incomingMessageQueue = new ArrayBlockingQueue<>(4);

		SwingUtilities.invokeLater(() -> {
			view = new View();
			view.addSendButtonActionListener(_ -> {
				String message = view.getMessageTextFieldText();
				if (!message.isEmpty()) {
					try {
						outgoingMessageQueue.put(message);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			});
		});

		new Thread(() -> {
			while (true) {
				try {
					String message = incomingMessageQueue.take();
					SwingUtilities.invokeLater(() -> view.messageListModel.addElement(message));

				} catch (InterruptedException e) {
					break;
				}
			}
		}).start();

		client = new Client("127.0.0.1", 6969, outgoingMessageQueue, incomingMessageQueue);
	}

	@Override
	public void run() {
		new Thread(client).start();
	}
}
