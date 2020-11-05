package io.openems.common.jsonrpc.request;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

/**
 * Represents a JSON-RPC Request to authenticate with a Password.
 * 
 * This is used by UI to login with password-only at the Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithPassword",
 *   "params": {
 *     "username"?: string,
 *     "password": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithPasswordRequest extends JsonrpcRequest {

	public final static String METHOD = "authenticateWithPassword";

	public static AuthenticateWithPasswordRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		Optional<String> username = JsonUtils.getAsOptionalString(p, "username");
		String password = JsonUtils.getAsString(p, "password");
		return new AuthenticateWithPasswordRequest(r.getId(), username, password);
	}

	public static AuthenticateWithPasswordRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final Optional<String> username;
	private final String password;

	public AuthenticateWithPasswordRequest(UUID id, Optional<String> username, String password) {
		super(id, METHOD);
		this.username = username;
		this.password = password;
	}

	public AuthenticateWithPasswordRequest(Optional<String> username, String password) {
		this(UUID.randomUUID(), username, password);
	}

	public Optional<String> getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public JsonObject getParams() {
		JsonObjectBuilder result = JsonUtils.buildJsonObject() //
				.addProperty("password", this.password); //
		if (this.username.isPresent()) {
			result.addProperty("username", this.username.get()); //
		}
		return result.build();
	}
}
