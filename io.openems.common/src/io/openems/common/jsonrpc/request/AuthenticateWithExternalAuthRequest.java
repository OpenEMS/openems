package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to authenticate with a trusted external
 * authentication provider.
 *
 * <p>
 * This is used by Edge installations where the UI is protected by a trusted
 * reverse proxy that passes the authenticated user via HTTP headers during the
 * WebSocket handshake.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticateWithExternalAuth",
 *   "params": {}
 * }
 * </pre>
 */
public class AuthenticateWithExternalAuthRequest extends JsonrpcRequest {

	public static final String METHOD = "authenticateWithExternalAuth";

	/**
	 * Create {@link AuthenticateWithExternalAuthRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticateWithExternalAuthRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AuthenticateWithExternalAuthRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new AuthenticateWithExternalAuthRequest(r);
	}

	private AuthenticateWithExternalAuthRequest(JsonrpcRequest request) {
		super(request, AuthenticateWithExternalAuthRequest.METHOD);
	}

	public AuthenticateWithExternalAuthRequest() {
		super(AuthenticateWithExternalAuthRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject().build();
	}
}
