package io.openems.api.security;

public enum OpenemsRole {
	/*
	 * "user" generally has readonly access
	 */
	USER("user"), //
	/*
	 * owner is the owner of the system.
	 */
	OWNER("owner"), //
	/*
	 * installer is a qualified electrician with extended configuration access
	 */
	INSTALLER("installer"), //
	/*
	 * admin is allowed to do anything
	 */
	ADMIN("admin");

	private final String text;

	private OpenemsRole(final String text) {
		this.text = text;
	}

	@Override public String toString() {
		return text;
	}
}
