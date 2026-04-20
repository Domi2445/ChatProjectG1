package User.Login;
import User.Model.User;

public record LoginResult(Status status, String message, User user) {
    public static LoginResult success(User user){
        return new LoginResult(Status.SUCCESS, "", user);
    }
    public static LoginResult wrongCredentials(){
        return new LoginResult(Status.WRONG_CREDENTIALS,"Username oder Passswort falsch.",null);
    }
    public static LoginResult usernameTaken(){
        return new LoginResult(Status.USERNAME_TAKEN, "Dieser Username ist bereits vergeben", null);
    }
    public static LoginResult invalidInput(String message){
        return new LoginResult(Status.INVALID_INPUT, message, null);
    }
    public static LoginResult databaseError(){
        return new LoginResult(Status.DATABASE_ERROR, "Verbindung zur Datenbank fehlgeschlagen. Bitte Später erneut versuchen",null);
    }

	public boolean isSuccess() {
		return status == Status.SUCCESS;
	}

	public boolean hasUser() {
		return user != null;
	}
}
