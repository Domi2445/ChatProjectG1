package Server;

import Util.Message.DeleteMessage;
import Util.Message.Message;
import Util.Message.TextMessage;
import Util.SocketProxy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MessageBroker implements Runnable {
	private final BlockingQueue<Message> messageBrokerQueue;
	private final List<SocketProxy> clients;
	private final Map<String, Message> messagesById;

	public MessageBroker(BlockingQueue<Message> messageBrokerQueue, List<SocketProxy> clients) {
		this.messageBrokerQueue = messageBrokerQueue;
		this.clients = clients;
		this.messagesById = new ConcurrentHashMap<>();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Message message = handleMessage(messageBrokerQueue.take());
				if (message != null) {
					sendToAllClients(message);
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	private Message handleMessage(Message message) {
		if (message instanceof DeleteMessage deleteMessage) {
			return handleDeleteMessage(deleteMessage);
		}

		if (message instanceof TextMessage textMessage) {
			return handleTextMessage(textMessage);
		}

		messagesById.putIfAbsent(message.getMessageId(), message);
		return message;
	}

	private Message handleTextMessage(TextMessage textMessage) {
		Message oldMessage = messagesById.get(textMessage.getMessageId());

		if (oldMessage == null) {
			messagesById.put(textMessage.getMessageId(), textMessage);
			return textMessage;
		}

		if (!(oldMessage instanceof TextMessage oldTextMessage)) {
			return null;
		}

		if (!oldTextMessage.getSender().getIdentifier().equals(textMessage.getSender().getIdentifier())) {
			return null;
		}

		if (textMessage.getContent().isBlank() || oldTextMessage.getContent().equals(textMessage.getContent())) {
			return null;
		}

		TextMessage editedMessage = new TextMessage(
				oldTextMessage.getMessageId(),
				oldTextMessage.getSender(),
				textMessage.getContent(),
				true
		);
		messagesById.put(editedMessage.getMessageId(), editedMessage);
		return editedMessage;
	}

	private Message handleDeleteMessage(DeleteMessage deleteMessage) {
		Message oldMessage = messagesById.get(deleteMessage.getMessageId());

		if (oldMessage == null) {
			return null;
		}

		if (!oldMessage.getSender().getIdentifier().equals(deleteMessage.getSender().getIdentifier())) {
			return null;
		}

		messagesById.remove(deleteMessage.getMessageId());
		return deleteMessage;
	}

	private void sendToAllClients(Message message) {
		for (SocketProxy client : clients) {
			try {
				client.out.writeObject(message);
				client.out.flush();
			} catch (IOException e) {
				System.out.println("Fehler beim Senden:\n" + e);
			}
		}
	}
}
