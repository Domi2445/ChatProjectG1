package Util;

import User.Model.User;

public class TextMessage extends Message {
	private final String content;
	private String editedContent;
	private final long id;
	private boolean isEdited;

	public TextMessage(User sender, String content) {
		super(sender);
		this.content = content;
		this.id = System.currentTimeMillis() + (long)(Math.random() * 1000);
		this.isEdited = false;
	}

	public String getContent() {
		return editedContent != null ? editedContent : content;
	}

	public void setEditedContent(String editedContent) {
		this.editedContent = editedContent;
		this.isEdited = true;
	}

	public long getId() {
		return id;
	}

	public boolean isEdited() {
		return isEdited;
	}
}
