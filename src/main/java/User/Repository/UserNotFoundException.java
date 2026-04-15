package User.Repository;

public class UserNotFoundException extends RuntimeException
{

    private final String username;

    public UserNotFoundException(String username)
    {
        super("User nicht gefunden: " + username);
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }
}