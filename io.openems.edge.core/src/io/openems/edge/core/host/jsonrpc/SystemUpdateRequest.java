package io.openems.edge.core.host.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request to execute a system update.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "systemUpdate",
 *   "params": {}
 * }
 * </pre>
 */
public class SystemUpdateRequest extends JsonrpcRequest {

	public static final String METHOD = "systemUpdate";

	/**
	 * Parses a generic {@link JsonrpcRequest} to a {@link SystemUpdateRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link SystemUpdateRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static SystemUpdateRequest from(JsonrpcRequest r) throws OpenemsException {
		return new SystemUpdateRequest(r);
	}

	private SystemUpdateRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
