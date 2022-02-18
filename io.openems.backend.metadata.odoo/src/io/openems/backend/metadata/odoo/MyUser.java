package io.openems.backend.metadata.odoo;

import java.util.NavigableMap;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Role;

public class MyUser extends User {

	private final int odooId;

	public MyUser(int odooId, String login, String name, String token, Role globalRole,
			NavigableMap<String, Role> roles, String language) {
		super(login, name, token, globalRole, roles, language);
		this.odooId = odooId;
	}

	/**
	 * Gets the internal Odoo record ID.
	 *
	 * @return the odoo id
	 */
	public int getOdooId() {
		return this.odooId;
	}
}
