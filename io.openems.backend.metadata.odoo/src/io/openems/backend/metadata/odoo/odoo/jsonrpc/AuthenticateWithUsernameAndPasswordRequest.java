package io.openems.backend.metadata.odoo.odoo.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to authenticate with username and password.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "call",
 *   "params": {
 *     "db": string,
 *     "login": string,
 *     "password": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithUsernameAndPasswordRequest extends OdooCallRequest {

	private final String db;
	private final String login;
	private final String password;

	public AuthenticateWithUsernameAndPasswordRequest(String db, String login, String password) {
		super();
		this.db = db;
		this.login = login;
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("db", this.db) //
				.addProperty("login", this.login) //
				.addProperty("password", this.password) //
				.build();
	}

	@Override
	public String toString() {
		return "AuthenticateWithUsernameAndPasswordRequest [db=" + db + ", login=" + login + ", password=HIDDEN]";
	}
}
