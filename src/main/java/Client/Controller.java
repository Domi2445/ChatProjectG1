package Client;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Controller {
	private final BlockingQueue<String> outgoingMessageQueue;
	private final BlockingQueue<String> incomingMessageQueue;

	private View view;
	private Client client;

	public Controller() {
		this.outgoingMessageQueue = new ArrayBlockingQueue<>(4);
		this.incomingMessageQueue = new ArrayBlockingQueue<>(4);
	}

	public void initView(Stage stage) {
		view = new View(stage);

		view.getSendButton().setOnAction(e -> sendMessage());
		view.getMessageTextField().setOnAction(e -> sendMessage());
	}

	public void connectAndRun(String ip, int port) {
		try {
			client = new Client(ip, port, outgoingMessageQueue, incomingMessageQueue);
			new Thread(client, "ClientThread").start();

			Thread listener = new Thread(() -> {
				while (true) {
					try {
						String message = incomingMessageQueue.take();
						Platform.runLater(() -> {
							view.getMessages().add(message);
							view.getMessageListView().scrollTo(view.getMessages().size() - 1);
						});
					} catch (InterruptedException e) {
						break;
					}
				}
			}, "IncomingMessageListener");
			listener.setDaemon(true);
			listener.start();

		} catch (IOException e) {
			Platform.runLater(() ->
				view.getMessages().add("Verbindung fehlgeschlagen: " + e.getMessage())
			);
		}
	}

	private void sendMessage() {
		String text = view.getMessageTextField().getText().trim();
		if (!text.isEmpty()) {
			try {
				outgoingMessageQueue.put(text);
				view.getMessages().add("Du: " + text);
				view.getMessageListView().scrollTo(view.getMessages().size() - 1);
				view.getMessageTextField().clear();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
