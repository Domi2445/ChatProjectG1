package Util.Login;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptWrapper {
    public static String hash(String plainText)
    {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("BCryptWrapper - Password darf nicht leer sein.");
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean validate(String plainText, String hashedPwd)
    {
        if (plainText == null || plainText.isBlank() || hashedPwd == null || hashedPwd.isBlank()) return false;

        try {
            return BCrypt.checkpw(plainText, hashedPwd);
        } catch (Exception e)
        {
            System.out.println("BCrpytWrapper - "+e);
        }
        return false;
    }
}
