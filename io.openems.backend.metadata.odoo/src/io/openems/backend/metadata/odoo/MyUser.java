package io.openems.backend.metadata.odoo;

import java.util.NavigableMap;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class MyUser extends User {

	private final int odooId;

	public MyUser(int odooId, String login, String name, String token, Language language, Role globalRole,
			NavigableMap<String, Role> roles, boolean hasMultipleEdges, JsonObject settings) {
		super(login, name, token, language, globalRole, roles, hasMultipleEdges, settings);
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
