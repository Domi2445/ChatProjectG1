package User.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "contact_data")
public class ContactData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String firstname;

	@Column
	private String lastname;

	@Column
	private String email;

	@Column
	private long discordId;

	@Column
	private String website;

	public ContactData(String firstname, String lastname, String email, long discordId, String website) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.email = email;
		this.discordId = discordId;
		this.website = website;
	}

	// Needed for JPA
	public ContactData() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public String toString() {
		return "ContactData{" +
			"firstname='" + firstname + '\'' +
			", lastname='" + lastname + '\'' +
			", email='" + email + '\'' +
			", discordId=" + discordId +
			", website='" + website + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ContactData that = (ContactData) o;
		return discordId == that.discordId && Objects.equals(firstname, that.firstname) && Objects.equals(lastname, that.lastname) && Objects.equals(email, that.email) && Objects.equals(website, that.website);
	}

	@Override
	public int hashCode() {
		return Objects.hash(firstname, lastname, email, discordId, website);
	}
}
