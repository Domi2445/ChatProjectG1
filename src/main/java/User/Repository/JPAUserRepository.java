package User.Repository;

import User.Model.User;

import java.util.Optional;

public class JPAUserRepository implements UserRepository
{
    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.empty();
    }

    @Override
    public void createUser(User user) {

    }

    @Override
    public void updateUser(User user) {

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public boolean usernameExists(String username) {
        return false;
    }
}
