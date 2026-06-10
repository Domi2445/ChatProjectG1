package Server;

import User.Login.Status;
import User.Model.User;
import User.Repository.RepositoryException;
import User.Repository.UserRepository;
import User.Repository.UsernameAlreadyExistsException;
import Util.Login.BCryptWrapper;
import Util.Login.LoginValidator;
import Util.Network.Auth.LoginRequest;
import Util.Network.Auth.LoginResponse;
import Util.Network.Auth.RegisterRequest;
import Util.Network.Auth.RegisterResponse;
import Util.Network.ProfilePictureUpdate;

import java.util.Optional;
import java.util.UUID;

public class AuthHandler {
	private final UserRepository userRepository;
	private static final int MAX_PROFILE_PICTURE_SIZE = 500_000; // bytes

	public AuthHandler(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public boolean handleLogin(LoginRequest request, ClientProxy sender) {
		if (sender == null) return false;

		if (!LoginValidator.validateUsername(request.getUsername())
			|| !LoginValidator.validatePassword(request.getPassword())) {
			sender.tryEnqueuePacket(new LoginResponse(Status.INVALID_INPUT, "Ungültige Eingabe", null));
			return false;
		}

		try {
			Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
			if (userOpt.isEmpty()) {
				sender.tryEnqueuePacket(new LoginResponse(Status.WRONG_CREDENTIALS, "Username oder Passwort falsch", null));
				return false;
			}

			User user = userOpt.get();
			if (!BCryptWrapper.validate(request.getPassword(), user.getPasswordHash())) {
				sender.tryEnqueuePacket(new LoginResponse(Status.WRONG_CREDENTIALS, "Username oder Passwort falsch", null));
				return false;
			}

			sender.setUser(user);
			sender.tryEnqueuePacket(new LoginResponse(Status.SUCCESS, "Erfolgreich angemeldet", user));
			return true;
		} catch (RepositoryException e) {
			System.err.println("Login fehlgeschlagen (DB): " + e.getMessage());
			e.printStackTrace();
			sender.tryEnqueuePacket(new LoginResponse(Status.DATABASE_ERROR, "Datenbankfehler", null));
			return false;
		}
	}

	public void handleRegister(RegisterRequest request, ClientProxy sender) {
		if (sender == null) return;

		if (!LoginValidator.validateUsername(request.getUsername())
			|| !LoginValidator.validateDisplayname(request.getDisplayname())
			|| !LoginValidator.validatePassword(request.getPassword())) {
			sender.tryEnqueuePacket(new RegisterResponse(Status.INVALID_INPUT, "Ungültige Eingabe", null));
			return;
		}

		try {
			if (userRepository.usernameExists(request.getUsername())) {
				sender.tryEnqueuePacket(new RegisterResponse(Status.USERNAME_TAKEN, "Username bereits vergeben", null));
				return;
			}

			String hash = BCryptWrapper.hash(request.getPassword());
			User user = new User();
			user.setUsername(request.getUsername());
			user.setDisplayname(request.getDisplayname());
			user.setPasswordHash(hash);

			userRepository.createUser(user);
			sender.setUser(user);
			sender.tryEnqueuePacket(new RegisterResponse(Status.SUCCESS, "Erfolgreich registriert", user));
		} catch (UsernameAlreadyExistsException e) {
			sender.tryEnqueuePacket(new RegisterResponse(Status.USERNAME_TAKEN, "Username bereits vergeben", null));
		} catch (RepositoryException e) {
			System.err.println("Register fehlgeschlagen (DB): " + e.getMessage());
			sender.tryEnqueuePacket(new RegisterResponse(Status.DATABASE_ERROR, "Datenbankfehler", null));
		}
	}

	public ProfilePictureUpdate handleProfilePictureUpdate(ProfilePictureUpdate update, ClientProxy sender) {
		if (sender == null || sender.getUser() == null || update == null) {
			return null;
		}

		String username = sender.getUser().getUsername();
		if (username == null || !username.equals(update.getUsername())) {
			return null;
		}

		byte[] imageBytes = update.getImageBytes();
		if (imageBytes == null || imageBytes.length == 0 || imageBytes.length > MAX_PROFILE_PICTURE_SIZE) {
			return null;
		}
		String contentType = update.getContentType();
		if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
			return null;
		}

		try {
			User user = sender.getUser();
			user.setProfilePicture(imageBytes);
			user.setProfilePictureContentType(contentType);
			user.setProfilePictureUUID(UUID.randomUUID());

			userRepository.updateUser(user);
			sender.setUser(user);

			return new ProfilePictureUpdate(username, imageBytes, contentType);
		} catch (RepositoryException e) {
			System.err.println("Profilbild konnte nicht gespeichert werden: " + e.getMessage());
			return null;
		}
	}
}
