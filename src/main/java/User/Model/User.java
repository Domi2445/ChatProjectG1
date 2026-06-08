package User.Model;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

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
    @Lob
    @Column
    private byte[] profilePicture;
    @Column
    private String profilePictureContentType;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) //1:1 beziehung ein datensatz gehört genau einem User
    @JoinColumn(name = "contact_data_id") //Nichts anderes wie ein Join
    private ContactData contactData;

	public User() {}

	public User(String username) {
		this.username = username;
	}

    public User(String username, String displayname, String passwordHash, String statusMessage, String profileDescription, UUID profilePictureUUID, ContactData contactData) {
        this.username = username;
        this.displayname = displayname;
        this.passwordHash = passwordHash;
        this.statusMessage = statusMessage;
        this.profileDescription = profileDescription;
        this.profilePictureUUID = profilePictureUUID;
        this.contactData = contactData;
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


    public void setProfilePictureUUID(UUID profilePictureUUID) {
        this.profilePictureUUID = profilePictureUUID;
    }

    public byte[] getProfilePicture() {
        return profilePicture == null ? null : Arrays.copyOf(profilePicture, profilePicture.length);
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture == null ? null : Arrays.copyOf(profilePicture, profilePicture.length);
    }

    public String getProfilePictureContentType() {
        return profilePictureContentType;
    }

    public void setProfilePictureContentType(String profilePictureContentType) {
        this.profilePictureContentType = profilePictureContentType;
    }

    public ContactData getContactData() {
        return contactData;
    }

    public void setContactData(ContactData contactData) {
        this.contactData = contactData;
    }

	@Override //Kein Password Hash, da er sonst geleakt werden könnte in dem LOG
	public String toString() {
		return "User{" +
			"username='" + username + '\'' +
			", displayname='" + displayname + '\'' +
			", statusMessage='" + statusMessage + '\'' +
			", profileDescription='" + profileDescription + '\'' +
			", profilePictureUUID=" + profilePictureUUID +
			", hasProfilePicture=" + (profilePicture != null && profilePicture.length > 0) +
			", contactData=" + contactData +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(username, user.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

    public String getDisplayName()
	{
		return displayname;
    }
}
