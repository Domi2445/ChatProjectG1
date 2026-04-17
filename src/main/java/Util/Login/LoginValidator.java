package Util.Login;

public class LoginValidator {
	public static boolean validateUsername(String username)
	{
		if (username == null || username.isBlank())
			throw new IllegalArgumentException("Nutzername darf nicht leer sein.");

		if (username.length() < 3)
			throw new IllegalArgumentException("Nutzername muss mindestens drei Zeichen lang sein.");

		if (isValid(username))
			throw new IllegalArgumentException("Nutzername enthält unzulässige Sonderzeichen.");

		return true;
	}

	private static boolean isValid(String input) {
		return input != null && input.matches("^[a-zA-Z0-9_.]+$");
	}
}
