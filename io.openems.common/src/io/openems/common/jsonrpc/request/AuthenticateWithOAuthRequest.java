package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
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
public class AuthenticateWithOAuthRequest extends JsonrpcRequest {

	public static final String METHOD = "authenticateWithOAuth";

	/**
	 * Create {@link AuthenticateWithOAuthRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticateWithOAuthRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AuthenticateWithOAuthRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticateWithOAuthRequest(r, payload);
	}

	private final JsonrpcRequest payload;

	private AuthenticateWithOAuthRequest(JsonrpcRequest request, JsonrpcRequest payload) {
		super(request, AuthenticateWithOAuthRequest.METHOD);
		this.payload = payload;
	}

	public AuthenticateWithOAuthRequest(JsonrpcRequest payload) {
		super(AuthenticateWithOAuthRequest.METHOD);
		this.payload = payload;
	}

	public JsonrpcRequest getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}
}
