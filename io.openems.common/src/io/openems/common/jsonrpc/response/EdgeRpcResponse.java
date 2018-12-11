package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonElement;

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

	private final JsonrpcResponseSuccess payload;

	public EdgeRpcResponse(UUID id, JsonrpcResponseSuccess payload) {
		super(id);
		this.payload = payload;
	}

	@Override
	public JsonElement getResult() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
