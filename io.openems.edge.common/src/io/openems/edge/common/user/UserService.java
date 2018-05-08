package io.openems.edge.common.user;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * This component handles User authentication.
 */
@Component(scope = ServiceScope.SINGLETON)
public class UserService {

	public Optional<User> authenticate(String password) {
		return Optional.of(User.ADMIN);
		// TODO
	}

}
