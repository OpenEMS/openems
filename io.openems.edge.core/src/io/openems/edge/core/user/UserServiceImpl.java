package io.openems.edge.core.user;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.CheckSetupPassword;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.user.ManagedUser;
import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

/**
 * This component handles User authentication.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.User", //
		immediate = true, //
		scope = ServiceScope.SINGLETON, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL //
)
public class UserServiceImpl implements UserService {

	private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	@Reference
	protected Host host;

	@Reference
	protected ConfigurationAdmin configurationAdmin;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL, //
			target = "(enabled=true)")
	private volatile ControllerApiBackend backend;

	/**
	 * All configured users. Ordered as they are added.
	 */
	private final List<ManagedUser> users = new ArrayList<>();
	private final List<UserConfig> usersFromConfig = new ArrayList<>();

	private record UserConfig(//
			String id, String name, //
			Language language, Role role, //
			String passwordHash, String salt) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link UserServiceImpl.UserConfig}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UserServiceImpl.UserConfig> serializer() {
			return jsonObjectSerializer(UserServiceImpl.UserConfig.class, json -> {
				return new UserServiceImpl.UserConfig(//
						json.getString("id"), //
						json.getString("name"), //
						json.getEnum("language", Language.class), //
						json.getEnum("role", Role.class), //
						json.getString("passwordHash"), //
						json.getString("salt") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.addProperty("name", obj.name()) //
						.addProperty("language", obj.language()) //
						.addProperty("role", obj.role()) //
						.addProperty("passwordHash", obj.passwordHash()) //
						.addProperty("salt", obj.salt()) //
						.build();
			});
		}

	}

	@Activate
	private void activate(Config config) {
		this.initFromConfig(config);
	}

	@Modified
	private void modified(Config config) {
		this.initFromConfig(config);
	}

	@Deactivate
	private void deactivate() {
	}

	private void initFromConfig(Config config) {
		this.users.clear();
		this.usersFromConfig.clear();

		this.users.add(//
				new ManagedUser("admin", "Admin", Language.DEFAULT, Role.ADMIN, config.adminPassword(),
						config.adminSalt()));
		this.users.add(//
				new ManagedUser("installer", "Installer", Language.DEFAULT, Role.INSTALLER, config.installerPassword(),
						config.installerSalt()));
		this.users.add(//
				new ManagedUser("owner", "Owner", Language.DEFAULT, Role.OWNER, config.ownerPassword(),
						config.ownerSalt()));
		this.users.add(//
				new ManagedUser("guest", "Guest", Language.DEFAULT, Role.GUEST, config.guestPassword(),
						config.guestSalt()));

		if (config.users() == null || config.users().isBlank()) {
			return;
		}

		try {
			final var users = UserConfig.serializer().toListSerializer().deserialize(JsonUtils.parse(config.users()));
			this.usersFromConfig.addAll(users);

			for (var user : this.usersFromConfig) {
				this.users.add(new ManagedUser(user.id(), user.name(), user.language(), user.role(),
						user.passwordHash(), user.salt()));
			}
		} catch (OpenemsNamedException | OpenemsRuntimeException e) {
			this.log.warn("Unable to parse Users from config.", e);
		}
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

	@Override
	public void registerAdminUser(String setupKey, String username, String password) throws OpenemsNamedException {
		this.checkBackendSetupPassword(setupKey);

		final var salt = getRandomSalt(16);
		final var saltEncoded = Base64.getEncoder().encodeToString(salt);
		final var passwordHash = ManagedUser.hashPassword(password, salt, ManagedUser.ITERATIONS,
				ManagedUser.KEY_LENGTH);
		final var passwordEncoded = Base64.getEncoder().encodeToString(passwordHash);

		final var user = new ManagedUser(username, username, Language.DEFAULT, Role.ADMIN, passwordEncoded,
				saltEncoded);

		// replace user if existing
		this.users.removeIf(u -> u.getName().equals(username));
		this.users.add(user);
		this.usersFromConfig.add(
				new UserConfig(username, username, user.getLanguage(), user.getRole(), passwordEncoded, saltEncoded));
		this.saveUsers();
	}

	private static byte[] getRandomSalt(int length) {
		SecureRandom sr = SecureRandomSingleton.getInstance();
		byte[] salt = new byte[length];
		sr.nextBytes(salt);
		return salt;
	}

	private void saveUsers() {
		try {
			final var configuration = this.configurationAdmin.getConfiguration("Core.User", "?");
			final var properties = configuration.getProperties() == null ? new Hashtable<String, Object>()
					: configuration.getProperties();
			properties.put("users", JsonUtils
					.prettyToString(UserConfig.serializer().toListSerializer().serialize(this.usersFromConfig)));
			configuration.updateIfDifferent(properties);
		} catch (IOException e) {
			this.log.warn("Unable to save user configuration.", e);
		}
	}

	private void checkBackendSetupPassword(String setupPassword) throws OpenemsNamedException {
		final var backend = this.backend;
		if (backend == null) {
			throw new RuntimeException("Backend not available");
		}

		try {
			backend.sendRequest(null, GenericJsonrpcRequest.createRequest(new CheckSetupPassword(),
					new CheckSetupPassword.Request(setupPassword))).get(30, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsNamedException(OpenemsError.COMMON_AUTHENTICATION_FAILED);
		}
	}

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
