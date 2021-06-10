package io.openems.backend.metadata.odoo;

public enum OdooUserRole {

	ADMIN("admin"), //
	INSTALLER("installer"), //
	OWNER("owner"), //
	GUEST("guest");

	private final String odooRole;
	
	OdooUserRole(String odooRole) {
		this.odooRole = odooRole;
	}
	
	public String getOdooRole() {
		return odooRole;
	}

}
