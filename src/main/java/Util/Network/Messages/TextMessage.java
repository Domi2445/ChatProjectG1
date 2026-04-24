package Util.Network.Messages;

import User.Model.User;

import java.io.Serial;

public class TextMessage extends Message {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String content;
	private String editedContent;
	private boolean isEdited;
	private boolean isDeleted;

    public TextMessage(User sender, String content) {
        super(sender);
        this.content = content;
        this.isEdited = false;
        this.isDeleted = false;
    }

    public String getContent() {
    	return editedContent != null ? editedContent : content;
    }

    public void setEditedContent(String editedContent) {
    	this.editedContent = editedContent;
    	this.isEdited = true;
    }

    public boolean isEdited() {
    	return isEdited;
    }

    public void setDeleted() {
    	this.isDeleted = true;
    }

    public boolean isDeleted() {
    	return isDeleted;
    }
}
