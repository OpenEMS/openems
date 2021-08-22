package io.openems.common.jsonrpc.request;

import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

/**
 * Represents a JSON-RPC Request to authenticate with a Password.
 * 
 * <p>
 * This is used by UI to login with username + password at Edge or Backend.
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

	public static final String METHOD = "authenticateWithPassword";

	/**
	 * Create {@link AuthenticateWithPasswordRequest} from a template
	 * {@link JsonrpcRequest}.
	 * 
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticateWithPasswordRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AuthenticateWithPasswordRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		Optional<String> username = JsonUtils.getAsOptionalString(p, "username");
		String password = JsonUtils.getAsString(p, "password");
		return new AuthenticateWithPasswordRequest(r, username, password);
	}

	private final Optional<String> username;
	private final String password;

	private AuthenticateWithPasswordRequest(JsonrpcRequest request, Optional<String> username, String password) {
		super(request, METHOD);
		this.username = username;
		this.password = password;
	}

	public AuthenticateWithPasswordRequest(Optional<String> username, String password) {
		super(METHOD);
		this.username = username;
		this.password = password;
	}

	/**
	 * Gets the Username if given.
	 * 
	 * @return Username
	 */
	public Optional<String> getUsername() {
		return this.username;
	}

	/**
	 * Gets the Password.
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return this.password;
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
