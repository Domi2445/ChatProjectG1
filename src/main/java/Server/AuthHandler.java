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

import java.util.Optional;

public class AuthHandler {
	private final UserRepository userRepository;

	public AuthHandler(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public void handleLogin(LoginRequest request, ClientProxy sender) {
		if (sender == null) return;

		if (!LoginValidator.validateUsername(request.getUsername())
			|| !LoginValidator.validatePassword(request.getPassword())) {
			sender.tryEnqueuePacket(new LoginResponse(Status.INVALID_INPUT, "Ungültige Eingabe", null));
			return;
		}

		try {
			Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
			if (userOpt.isEmpty()) {
				sender.tryEnqueuePacket(new LoginResponse(Status.WRONG_CREDENTIALS, "Username oder Passwort falsch", null));
				return;
			}

			User user = userOpt.get();
			if (!BCryptWrapper.validate(request.getPassword(), user.getPasswordHash())) {
				sender.tryEnqueuePacket(new LoginResponse(Status.WRONG_CREDENTIALS, "Username oder Passwort falsch", null));
				return;
			}

			sender.setUser(user);
			sender.tryEnqueuePacket(new LoginResponse(Status.SUCCESS, "Erfolgreich angemeldet", user));
		} catch (RepositoryException e) {
			System.err.println("Login fehlgeschlagen (DB): " + e.getMessage());
			sender.tryEnqueuePacket(new LoginResponse(Status.DATABASE_ERROR, "Datenbankfehler", null));
		}
	}

	public void handleRegister(RegisterRequest request, ClientProxy sender) {
		if (sender == null) return;

		if (!LoginValidator.validateUsername(request.getUsername())
			|| !LoginValidator.validateDisplayname(request.getDisplayname())
			|| !LoginValidator.validatePassword(request.getPassword())) {
			sender.tryEnqueuePacket(new RegisterResponse(Status.INVALID_INPUT, "Ungültige Eingabe"));
			return;
		}

		try {
			if (userRepository.usernameExists(request.getUsername())) {
				sender.tryEnqueuePacket(new RegisterResponse(Status.USERNAME_TAKEN, "Username bereits vergeben"));
				return;
			}

			String hash = BCryptWrapper.hash(request.getPassword());
			User user = new User();
			user.setUsername(request.getUsername());
			user.setDisplayname(request.getDisplayname());
			user.setPasswordHash(hash);

			userRepository.createUser(user);
			sender.tryEnqueuePacket(new RegisterResponse(Status.SUCCESS, "Erfolgreich registriert"));
		} catch (UsernameAlreadyExistsException e) {
			sender.tryEnqueuePacket(new RegisterResponse(Status.USERNAME_TAKEN, "Username bereits vergeben"));
		} catch (RepositoryException e) {
			System.err.println("Register fehlgeschlagen (DB): " + e.getMessage());
			sender.tryEnqueuePacket(new RegisterResponse(Status.DATABASE_ERROR, "Datenbankfehler"));
		}
	}
}
