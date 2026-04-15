package User;

public class User {
    private String username;
    private String displayname;
    private String passwordHash;
    private String statusMessage;
    private String profileDescription;
    private String profilePicturePath;
    private ContactData contactData;

    public User(String username, String displayname, String passwordHash, String statusMessage, String profileDescription, String profilePicturePath, ContactData contactData) {
        this.username = username;
        this.displayname = displayname;
        this.passwordHash = passwordHash;
        this.statusMessage = statusMessage;
        this.profileDescription = profileDescription;
        this.profilePicturePath = profilePicturePath;
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

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    public ContactData getContactData() {
        return contactData;
    }

    public void setContactData(ContactData contactData) {
        this.contactData = contactData;
    }
}
