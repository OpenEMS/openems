package io.openems.common.session;

public enum Role {
	ADMIN, INSTALLER, OWNER, GUEST;

	public static Role getRole(String name) {
		switch (name.toLowerCase()) {
		case "admin":
			return ADMIN;
		case "installer":
			return INSTALLER;
		case "owner":
			return OWNER;
		case "guest":
		default:
			return GUEST;
		}
	}
	
	public static Role getDefaultRole() {
		return GUEST;
	}
}
