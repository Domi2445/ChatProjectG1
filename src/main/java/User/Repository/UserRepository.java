package User.Repository;

import User.Model.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    void createUser(User user);

    void updateUser(User user);

    void deleteUser(String username);

    boolean usernameExists(String username);
}
