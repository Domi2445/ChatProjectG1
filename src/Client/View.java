package Client;

import javax.swing.*;
import java.awt.event.ActionListener;

public class View extends JFrame {
	private JPanel contentPanel;
	private JList<String> messageList;
	private JTextField messageTextField;
	private JButton sendButton;

	public DefaultListModel<String> messageListModel;

	public View() {
		setContentPane(contentPanel);
		setTitle("Socket Chat Client");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void createUIComponents() {
		messageListModel = new DefaultListModel<>();
		messageList = new JList<>(messageListModel);
	}

	public String getMessageTextFieldText() {
		return messageTextField.getText();
	}

	public void addSendButtonActionListener(ActionListener l) {
		sendButton.addActionListener(l);
	}
}
