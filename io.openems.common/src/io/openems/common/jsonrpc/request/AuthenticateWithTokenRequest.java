package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to authenticate with a Token.
 *
 * <p>
 * This is used by UI to login with a Token at Edge or Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithToken",
 *   "params": {
 *     "token": string
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithTokenRequest extends JsonrpcRequest {

	public static final String METHOD = "authenticateWithToken";

	/**
	 * Create {@link AuthenticateWithTokenRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticateWithTokenRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AuthenticateWithTokenRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var token = JsonUtils.getAsString(p, "token");
		return new AuthenticateWithTokenRequest(r, token);
	}

	private final String token;

	private AuthenticateWithTokenRequest(JsonrpcRequest request, String token) {
		super(request, AuthenticateWithTokenRequest.METHOD);
		this.token = token;
	}

	public AuthenticateWithTokenRequest(String token) {
		super(AuthenticateWithTokenRequest.METHOD);
		this.token = token;
	}

	/**
	 * Gets the Token.
	 *
	 * @return Token
	 */
	public String getToken() {
		return this.token;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token) //
				.build();
	}
}
