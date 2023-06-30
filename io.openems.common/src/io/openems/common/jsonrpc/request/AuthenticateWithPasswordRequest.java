package io.openems.common.jsonrpc.request;

import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

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
		var p = r.getParams();
		var usernameOpt = JsonUtils.getAsOptionalString(p, "username");
		var password = JsonUtils.getAsString(p, "password");
		return new AuthenticateWithPasswordRequest(r, usernameOpt, password);
	}

	/**
	 * The Username if given.
	 *
	 * @return Username
	 */
	public final Optional<String> usernameOpt;

	/**
	 * The Password.
	 *
	 * @return Password
	 */
	public final String password;

	private AuthenticateWithPasswordRequest(JsonrpcRequest request, Optional<String> usernameOpt, String password) {
		super(request, AuthenticateWithPasswordRequest.METHOD);
		this.usernameOpt = usernameOpt;
		this.password = password;
	}

	public AuthenticateWithPasswordRequest(Optional<String> usernameOpt, String password) {
		super(AuthenticateWithPasswordRequest.METHOD);
		this.usernameOpt = usernameOpt;
		this.password = password;
	}

	@Override
	public JsonObject getParams() {
		var result = JsonUtils.buildJsonObject() //
				.addProperty("password", this.password); //
		if (this.usernameOpt.isPresent()) {
			result.addProperty("username", this.usernameOpt.get()); //
		}
		return result.build();
	}
}
