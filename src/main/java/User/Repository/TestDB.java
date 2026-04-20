package User.Repository;

import User.Model.User;

import java.util.Scanner;

public class TestDB {

	public static void main(String[] args) {
		// Fuer lokale Entwicklung: Schema bei Bedarf automatisch erzeugen/aktualisieren.
		System.setProperty("db.ddl", "update");

		JPAUserRepository repo = new JPAUserRepository();

		try {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Bitte Usernamen eingeben:");
			String username = scanner.nextLine();
			repo.createUser(new User(username));
			repo.usernameExists(username);

		} catch (Exception e) {
			System.out.println("Fehler: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
