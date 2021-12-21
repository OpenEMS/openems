package io.openems.edge.common.test;

import io.openems.common.session.Role;
import io.openems.edge.common.user.User;

/**
 * Simulates a {@link User} for the OpenEMS Component test framework.
 */
public class DummyUser extends User {

	protected final String password;

	public DummyUser(String id, String password, Role role) {
		super(id, id, role);
		this.password = password;
	}
}