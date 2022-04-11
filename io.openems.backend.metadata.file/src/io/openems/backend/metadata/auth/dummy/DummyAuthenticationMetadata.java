package io.openems.backend.metadata.auth.dummy;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.AuthenticationMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "AuthenticationMetadata.Dummy", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DummyAuthenticationMetadata extends AbstractOpenemsBackendComponent implements AuthenticationMetadata {
	
	private static final String USER_ID = "admin";
	private static final String USER_NAME = "Administrator";
	private static final Role USER_GLOBAL_ROLE = Role.ADMIN;
	private static Language LANGUAGE;
	private User user;

	public DummyAuthenticationMetadata() {
		super("AuthenticationMetadata.Dummy");
		this.user = DummyAuthenticationMetadata.generateUser();
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		return this.user;
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		if (this.user.getToken().equals(token)) {
			return this.user;
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	@Override
	public void logout(User user) {
		this.user = DummyAuthenticationMetadata.generateUser();
	}

	private static User generateUser() {
		return new User(DummyAuthenticationMetadata.USER_ID, DummyAuthenticationMetadata.USER_NAME, UUID.randomUUID().toString(),
				DummyAuthenticationMetadata.LANGUAGE, DummyAuthenticationMetadata.USER_GLOBAL_ROLE, new TreeMap<>());
	}
	
	@Override
	public Optional<User> getUser(String userId) {
		return Optional.of(this.user);
	}
	
	@Override
	public void updateUserLanguage(User user, Language locale) throws OpenemsNamedException {
		DummyAuthenticationMetadata.LANGUAGE = locale;
	}

	@Override
	public void registerUser(JsonObject jsonObject) throws OpenemsNamedException {
		throw new IllegalArgumentException("DummyAuthenticationMetadata.registerUser() is not implemented");
	}
	
	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		throw new NotImplementedException("DummyAuthenticationMetadata.getUserInformation()");
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new NotImplementedException("DummyAuthenticationMetadata.setUserInformation()");
	}
	
	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		throw new NotImplementedException("FileMetadata.addEdgeToUser()");
	}
	
}
