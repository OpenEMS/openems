package io.openems.backend.metadata.odoo;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class MyUser extends User {

	private final int odooId;

	public MyUser(int odooId, String userId, String email, String name, String token, Language language,
			Role globalRole, boolean hasMultipleEdges, JsonObject settings) {
		super(userId, email, name, token, language, globalRole, hasMultipleEdges, settings);
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
