package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request to logout.
 *
 * <p>
 * This is used by UI to logout from Edge or Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "logout",
 *   "params": {}
 * }
 * </pre>
 */
public class LogoutRequest extends JsonrpcRequest {

	public static final String METHOD = "logout";

	/**
	 * Create {@link LogoutRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link LogoutRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static LogoutRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new LogoutRequest(r);
	}

	private LogoutRequest(JsonrpcRequest request) {
		super(request, LogoutRequest.METHOD);
	}

	public LogoutRequest() {
		super(LogoutRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}
}
