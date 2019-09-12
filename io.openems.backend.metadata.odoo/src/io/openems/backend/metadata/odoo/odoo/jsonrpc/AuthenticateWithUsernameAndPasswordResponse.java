package io.openems.backend.metadata.odoo.odoo.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a successful JSON-RPC Response for
 * 'AuthenticateWithUsernameAndPasswordRequest'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "username": string,
 *     "uid": number,
 *     "name": string,
 *     "session_id": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithUsernameAndPasswordResponse extends JsonrpcResponseSuccess {

	public static AuthenticateWithUsernameAndPasswordResponse from(JsonrpcResponseSuccess response)
			throws OpenemsNamedException {
		JsonObject r = response.getResult();
		String username;
		try {
			username = JsonUtils.getAsString(r, "username");
		} catch (OpenemsNamedException e) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		int uid = JsonUtils.getAsInt(r, "uid");
		String name = JsonUtils.getAsString(r, "name");
		String sessionId = JsonUtils.getAsString(r, "session_id");
		return new AuthenticateWithUsernameAndPasswordResponse(response.getId(), username, uid, name, sessionId);
	}

	public static AuthenticateWithUsernameAndPasswordResponse from(JsonObject j) throws OpenemsNamedException {
		return from(JsonrpcResponseSuccess.from(j));
	}

	private final String username;
	private final int uid;
	private final String name;
	private final String sessionId;

	public AuthenticateWithUsernameAndPasswordResponse(UUID id, String username, int uid, String name,
			String sessionId) {
		super(id);
		this.username = username;
		this.uid = uid;
		this.name = name;
		this.sessionId = sessionId;
	}

	public String getUsername() {
		return username;
	}

	public int getUid() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public String getSessionId() {
		return sessionId;
	}

	@Override
	public JsonObject getResult() {
		// TODO
		return new JsonObject();
	}

}
