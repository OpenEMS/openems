package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to authenticate with a Password.
 * This is used by UI to login with password-only at the Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithPassword",
 *   "params": {
 *     "password": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithPasswordRequest extends JsonrpcRequest {

	public static final String METHOD = "authenticateWithPassword";

	public static AuthenticateWithPasswordRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String password = JsonUtils.getAsString(p, "password");
		return new AuthenticateWithPasswordRequest(r.getId(), password);
	}

	public static AuthenticateWithPasswordRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final String password;

	public AuthenticateWithPasswordRequest(UUID id, String password) {
		super(id, METHOD);
		this.password = password;
	}

	public AuthenticateWithPasswordRequest(String password) {
		this(UUID.randomUUID(), password);
	}

	public String getPassword() {
		return password;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("password", this.password) //
				.build();
	}
}
