package User.Repository;

import User.Model.User;
import User.Repository.JPAUserRepository;

public class TestDB {

    public static void main(String[] args) {

        JPAUserRepository repo = new JPAUserRepository();

        try {
            // CREATE
            User user1 = new User();
            user1.setUsername("max");

            repo.createUser(user1);
            System.out.println("User erstellt: max");

            //  EXISTS
            boolean exists = repo.usernameExists("max");
            System.out.println("Existiert max? " + exists);

            // FIND
            repo.findByUsername("max")
                    .ifPresentOrElse(
                            u -> System.out.println("Gefunden: " + u.getUsername()),
                            () -> System.out.println("User nicht gefunden")
                    );

            //  UPDATE
            user1.setUsername("max"); // hier könntest du weitere Felder ändern
            repo.updateUser(user1);
            System.out.println("User aktualisiert");

            //  DELETE
            repo.deleteUser("max");
            System.out.println("User gelöscht");

            // CHECK AGAIN
            boolean existsAfter = repo.usernameExists("max");
            System.out.println("Existiert nach Löschung? " + existsAfter);

        } catch (Exception e) {
            System.out.println("Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}