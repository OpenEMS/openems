package io.openems.edge.common.test;

import static io.openems.common.session.Language.DEFAULT;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.user.User;

/**
 * Simulates a {@link User} for the OpenEMS Component test framework.
 */
public class DummyUser extends User {

	private static final String GUEST = "guest";
	private static final String OWNER = "owner";
	private static final String INSTALLER = "installer";
	private static final String ADMIN = "admin";

	public static final DummyUser DUMMY_GUEST = new DummyUser(GUEST, GUEST, DEFAULT, Role.GUEST);
	public static final DummyUser DUMMY_OWNER = new DummyUser(OWNER, OWNER, DEFAULT, Role.OWNER);
	public static final DummyUser DUMMY_INSTALLER = new DummyUser(INSTALLER, INSTALLER, DEFAULT, Role.INSTALLER);
	public static final DummyUser DUMMY_ADMIN = new DummyUser(ADMIN, ADMIN, DEFAULT, Role.ADMIN);

	public final String password;

	private DummyUser(String id, String password, Language language, Role role) {
		super(id, id, language, role);
		this.password = password;
	}
}