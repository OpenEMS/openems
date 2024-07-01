package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Notification for a specific Edge-ID.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeRpc",
 *   "params": {
 *     "edgeId": string,
 *     "payload": JsonrpcNotification
 *   }
 * }
 * </pre>
 */
public class EdgeRpcNotification extends JsonrpcNotification {

	public static final String METHOD = "edgeRpc";

	private final String edgeId;
	private final JsonrpcNotification payload;

	public EdgeRpcNotification(String edgeId, JsonrpcNotification payload) {
		super(EdgeRpcNotification.METHOD);
		this.edgeId = edgeId;
		this.payload = payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

	public String getEdgeId() {
		return this.edgeId;
	}

	public JsonrpcNotification getPayload() {
		return this.payload;
	}

}
