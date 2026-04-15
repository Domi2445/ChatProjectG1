package User.Model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ContactData {
    private String firstname;
    private String lastname;
    private String email;
    private long discordId;
    private String website;

    public ContactData(String firstname, String lastname, String email, long discordId, String website) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.discordId = discordId;
        this.website = website;
    }

    public ContactData() {

    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
