package Util.Login;

import Logs.LogHandler;

public class LoginValidator {
	public static boolean validateUsername(String username)
	{
		if (username == null || username.isBlank()) {
			LogHandler.log("LoginValidator", "Displayname darf nicht leer sein.");
			return false;
		}
		if (username.length() < 3) {
			LogHandler.log("LoginValidator", "Displayname muss mindestens drei Zeichen lang sein.");
			return false;
		}
		if (isValid(username)) {
			LogHandler.log("LoginValidator", "Nutzername enthält unzulässige Sonderzeichen.");
			return false;
		}
		return true;
	}

	public static boolean validateDisplayname(String displayName)
	{
		if (displayName == null || displayName.isBlank()) {
			LogHandler.log("LoginValidator", "Displayname darf nicht leer sein.");
			return false;
		}
		if (displayName.length() < 3) {
			LogHandler.log("LoginValidator", "Displayname muss mindestens drei Zeichen lang sein.");
			return false;
		}
		if (isValid(displayName)) {
			LogHandler.log("LoginValidator", "Nutzername enthält unzulässige Sonderzeichen.");
			return false;
		}
		return true;
	}

	public static boolean validatePassword(String password)
	{
		if (password.length()<8) {
			LogHandler.log("LoginValidator", "Passwort muss mindestens 8 Zeichen lang sein.");
			return false;
		}
		//Bin ich auch kein Fan von
		if (password.length() > 64) {
			LogHandler.log("LoginValidator", "Passwort darf nicht länger als 64 Zeichen sein.");
			return false;
		}
		return true;
	}

	private static boolean isValid(String input) {
		return input != null && input.matches("^[a-zA-Z0-9_.]+$");
	}
}
