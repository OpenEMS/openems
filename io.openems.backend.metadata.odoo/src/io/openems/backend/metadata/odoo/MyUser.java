package io.openems.backend.metadata.odoo;

import java.util.NavigableMap;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Role;

public class MyUser extends User {

	private final int odooId;

	public MyUser(int odooId, String name, Role globalRole, NavigableMap<String, Role> roles) {
		super(String.valueOf(odooId), name, globalRole, roles);
		this.odooId = odooId;
	}

	public int getOdooId() {
		return odooId;
	}
}
