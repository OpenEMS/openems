package io.openems.backend.metadata.odoo;

/**
 * Represents a Backend-User within Metadata Service.
 */
public class MyUser {

	private final int odooId;
	private final String id;
	private final String name;
	private final String sessionId;

	public MyUser(int odooId, String name, String sessionId) {
		this.id = String.valueOf(odooId);
		this.odooId = odooId;
		this.name = name;
		this.sessionId = sessionId;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSessionId() {
		return sessionId;
	}

	public int getOdooId() {
		return odooId;
	}

}
