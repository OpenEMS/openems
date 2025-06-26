package io.openems.common.jsonrpc.response;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'edgeRpc'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": JsonrpcResponse
 *   }
 * }
 * </pre>
 */
public class EdgeRpcResponse extends JsonrpcResponseSuccess {

	/**
	 * Parses a {@link JsonrpcResponseSuccess} to a {@link EdgeRpcResponse}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return the {@link EdgeRpcResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static EdgeRpcResponse from(JsonrpcResponseSuccess r) throws OpenemsNamedException {
		var p = r.getResult();
		var payload = JsonrpcResponseSuccess.from(getAsJsonObject(p, "payload"));
		return new EdgeRpcResponse(r.getId(), payload);
	}

	private final JsonrpcResponseSuccess payload;

	public EdgeRpcResponse(UUID id, JsonrpcResponseSuccess payload) {
		super(id);
		this.payload = payload;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
