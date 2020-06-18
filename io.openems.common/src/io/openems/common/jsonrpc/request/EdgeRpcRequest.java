package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request for a specific Edge-ID.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "edgeRpc",
 *   "params": {
 *     "edgeId": string,
 *     "payload": {
 *     }
 *   }
 * }
 * </pre>
 */
public class EdgeRpcRequest extends JsonrpcRequest {

	public final static String METHOD = "edgeRpc";

	public static EdgeRpcRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String edgeId = JsonUtils.getAsString(p, "edgeId");
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new EdgeRpcRequest(r.getId(), edgeId, payload);
	}

	private final String edgeId;
	private final JsonrpcRequest payload;

	public EdgeRpcRequest(String edgeId, JsonrpcRequest payload) {
		this(UUID.randomUUID(), edgeId, payload);
	}

	public EdgeRpcRequest(UUID id, String edgeId, JsonrpcRequest payload) {
		super(id, METHOD);
		this.edgeId = edgeId;
		this.payload = payload;
	}

	public String getEdgeId() {
		return edgeId;
	}

	public JsonrpcRequest getPayload() {
		return payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
