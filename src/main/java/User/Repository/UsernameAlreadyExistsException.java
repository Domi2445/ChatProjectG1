package User.Repository;

public class UsernameAlreadyExistsException extends RuntimeException
{

    private final String username;

    public UsernameAlreadyExistsException(String username)
    {
        super("Username bereits vergeben: " + username);
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }
}