package Client;

import Util.Message;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Controller {
	private final BlockingQueue<Message> outgoingMessageQueue;
	private final BlockingQueue<Message> incomingMessageQueue;

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
						Message message = incomingMessageQueue.take();
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
					view.getMessages().add(new Message("System", "Verbindung fehlgeschlagen: " + e.getMessage()))
			);
		}
	}

	// Controller.java - sendMessage()
	private void sendMessage() {
		String text = view.getMessageTextField().getText().trim();
		if (!text.isEmpty()) {
			try {
				Message message = new Message("Du", text);
				outgoingMessageQueue.put(message);
				// view.getMessages().add(message);  ← diese Zeile löschen!
				view.getMessageListView().scrollTo(view.getMessages().size() - 1);
				view.getMessageTextField().clear();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
