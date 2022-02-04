package io.openems.common.jsonrpc.request;

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

	public static final String METHOD = "edgeRpc";

	/**
	 * Create {@link EdgeRpcRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link EdgeRpcRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static EdgeRpcRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var edgeId = JsonUtils.getAsString(p, "edgeId");
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new EdgeRpcRequest(r, edgeId, payload);
	}

	private final String edgeId;
	private final JsonrpcRequest payload;

	public EdgeRpcRequest(String edgeId, JsonrpcRequest payload) {
		super(EdgeRpcRequest.METHOD, payload.getTimeout() /* inherit timeout from payload */);
		this.edgeId = edgeId;
		this.payload = payload;
	}

	public EdgeRpcRequest(JsonrpcRequest request, String edgeId, JsonrpcRequest payload) {
		super(request, EdgeRpcRequest.METHOD);
		this.edgeId = edgeId;
		this.payload = payload;
	}

	/**
	 * Gets the Edge-ID.
	 *
	 * @return Edge-ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

	/**
	 * Gets the Payload {@link JsonrpcRequest}.
	 *
	 * @return Payload
	 */
	public JsonrpcRequest getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
