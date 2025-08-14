package io.openems.common.jsonrpc.notification;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
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

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link EdgeRpcNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link EdgeConfigNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static EdgeRpcNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		var p = n.getParams();
		var edgeId = getAsString(p, "edgeId");
		var payload = GenericJsonrpcNotification.from(getAsJsonObject(p, "payload"));
		return new EdgeRpcNotification(edgeId, payload);
	}

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
