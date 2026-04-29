package Util.Login;

import Logs.LogHandler;
import org.mindrot.jbcrypt.BCrypt;

public class BCryptWrapper {
    public static String hash(String plainText)
    {
        if (plainText == null || plainText.isBlank()) {
			LogHandler.log("BCryptWrapper", "Password darf nicht leer sein.");
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
			LogHandler.error("BCryptWrapper", e.getMessage());
        }
        return false;
    }
}
