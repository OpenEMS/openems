package io.openems.backend.metadata.odoo;

import io.openems.backend.metadata.api.BackendUser;

public class MyUser extends BackendUser {

	private final int odooId;

	public MyUser(int odooId, String name, String sessionId) {
		super(String.valueOf(odooId), name, sessionId);
		this.odooId = odooId;
	}

	public int getOdooId() {
		return odooId;
	}
}
