package io.openems.edge.core.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.user.ManagedUser;
import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;

/**
 * This component handles User authentication.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.User", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL //
)
public class UserServiceImpl implements UserService {

	private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	@Reference
	protected Host host;

	/**
	 * All configured users. Ordered as they are added.
	 */
	private final List<ManagedUser> users = new ArrayList<>();

	@Activate
	void activate(Config config) {
		this.initializeUser("admin", "Admin", Language.DEFAULT, Role.ADMIN, config.adminPassword(), config.adminSalt());
		this.users.add(//
				new ManagedUser("installer", "Installer", Language.DEFAULT, Role.INSTALLER, config.installerPassword(),
						config.installerSalt()));
		this.users.add(//
				new ManagedUser("owner", "Owner", Language.DEFAULT, Role.OWNER, config.ownerPassword(),
						config.ownerSalt()));
		this.users.add(//
				new ManagedUser("guest", "Guest", Language.DEFAULT, Role.GUEST, config.guestPassword(),
						config.guestSalt()));
	}

	@Deactivate
	void deactivate() {
	}

	@Override
	public final Optional<User> authenticate(String username, String password) {
		// Search for user with given username
		for (ManagedUser user : this.users) {
			if (username.equals(user.getName())) {
				if (user.validatePassword(password)) {
					this.log.info("Authentication successful for user[" + username + "].");
					return Optional.of(user);
				}
				this.log.info("Authentication failed for user[" + username + "]: wrong password");
				return Optional.empty();
			}
		}
		// Try authenticating with password only
		return this.authenticate(password);
	}

	@Override
	public final Optional<User> authenticate(String password) {
		// Search for any user with the given password
		for (ManagedUser user : this.users) {
			if (user.validatePassword(password)) {
				this.log.info("Authentication successful with password only for user [" + user.getName() + "].");
				return Optional.ofNullable(user);
			}
		}
		this.log.info("Authentication failed with password only.");
		return Optional.empty();
	}

	/**
	 * Initializes the User; makes sure there is a safe password in case the
	 * password is the same as the role.
	 * 
	 * @param id       the user id, e.g. "admin"
	 * @param name     a user name, e.g#. "Admin"
	 * @param language the {@link Language}
	 * @param role     the user {@link Role}
	 * @param password the base64 hashed password
	 * @param salt     the base64 hashed salt
	 */
	private void initializeUser(String id, String name, Language language, Role role, String password, String salt) {
		if (
		// Is this a FEMS (i.e. no development environment)?
		this.host.getHostname().orElse(//
				this.host.getHostnameChannel().getNextValue().orElse(//
						"UNKNOWN"))
				.startsWith("fems")
				// Does the password match the id (i.e. the password is insecure)?
				&& ManagedUser.validatePassword(password, salt, role.toString().toLowerCase())) {
			// -> yes. Replace with generated password.
			byte[] generatedSalt = UserServiceUtils.generateSalt(this.host, id);
			byte[] generatedPassword = ManagedUser.hashPassword(UserServiceUtils.generatePassword(this.host, id, role),
					generatedSalt, ManagedUser.ITERATIONS, ManagedUser.KEY_LENGTH);
			this.users.add(//
					new ManagedUser(id, name, language, role, generatedPassword, generatedSalt));
		} else {
			this.users.add(//
					new ManagedUser(id, name, language, role, password, salt));
		}
	}

	// private static byte[] getRandomSalt(int length) {
	// SecureRandom sr = SecureRandomSingleton.getInstance();
	// byte[] salt = new byte[length];
	// sr.nextBytes(salt);
	// return salt;
	// }

	// TODO implement change password
	// public void changePassword(String oldPassword, String newPassword) throws
	// OpenemsException {
	// if (checkPassword(oldPassword)) {
	// byte[] salt = getRandomSalt(SALT_LENGTH);
	// byte[] password = hashPassword(newPassword, salt);
	// this.password = password;
	// this.salt = salt;
	// // Config.getInstance().writeConfigFile();
	// } else {
	// throw new OpenemsException("Access denied. Old password was wrong.");
	// }
	// }

}
