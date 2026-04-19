package User.Model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;
    @Column
    private String displayname;
    @Column
    private String passwordHash;
    @Column
    private String statusMessage;
    @Column
    private String profileDescription;
    @Column
    private UUID profilePictureUUID;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) //1:1 beziehung ein datensatz gehört genau einem User
    @JoinColumn(name = "contact_data_id") //Nichts anderes wie ein Join
    private ContactData contactData;

    public User(String username, String displayname, String passwordHash, String statusMessage, String profileDescription, UUID profilePictureUUID, ContactData contactData) {
        this.username = username;
        this.displayname = displayname;
        this.passwordHash = passwordHash;
        this.statusMessage = statusMessage;
        this.profileDescription = profileDescription;
        this.profilePictureUUID = profilePictureUUID;
        this.contactData = contactData;
    }

    public User() {

    }
	//TODO: Vorübergehend, da sonst fehler
	public User(String benutzername) {
	}

	public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getProfileDescription() {
        return profileDescription;
    }

    public void setProfileDescription(String profileDescription) {
        this.profileDescription = profileDescription;
    }

    public UUID getProfilePictureUUID() {
        return profilePictureUUID;
    }

    public void setProfilePictureUUI(UUID profilePictureUUID) {
        this.profilePictureUUID = profilePictureUUID;
    }

    public ContactData getContactData() {
        return contactData;
    }

    public void setContactData(ContactData contactData) {
        this.contactData = contactData;
    }

	@Override
	public String toString() {
		return "User{" +
			"username='" + username + '\'' +
			", displayname='" + displayname + '\'' +
			", passwordHash='" + passwordHash + '\'' +
			", statusMessage='" + statusMessage + '\'' +
			", profileDescription='" + profileDescription + '\'' +
			", profilePictureUUID=" + profilePictureUUID +
			", contactData=" + contactData +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(username, user.username) && Objects.equals(displayname, user.displayname) && Objects.equals(passwordHash, user.passwordHash) && Objects.equals(statusMessage, user.statusMessage) && Objects.equals(profileDescription, user.profileDescription) && Objects.equals(profilePictureUUID, user.profilePictureUUID) && Objects.equals(contactData, user.contactData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username, displayname, passwordHash, statusMessage, profileDescription, profilePictureUUID, contactData);
	}
}
