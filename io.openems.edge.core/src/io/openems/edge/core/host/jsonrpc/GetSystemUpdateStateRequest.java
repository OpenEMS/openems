
package io.openems.edge.core.host.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Gets the System Update State.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSystemUpdateState",
 *   "params": {
 *   	"debug"?: boolean
 *   }
 * }
 * </pre>
 */
public class GetSystemUpdateStateRequest extends JsonrpcRequest {

	public static final String METHOD = "getSystemUpdateState";

	/**
	 * Parses a generic {@link JsonrpcRequest} to a
	 * {@link GetSystemUpdateStateRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link GetSystemUpdateStateRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GetSystemUpdateStateRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new GetSystemUpdateStateRequest(r);
	}

	public GetSystemUpdateStateRequest() {
		super(METHOD);
	}

	private GetSystemUpdateStateRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.build();
	}

}
