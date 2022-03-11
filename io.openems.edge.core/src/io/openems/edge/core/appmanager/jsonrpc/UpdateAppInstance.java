package io.openems.edge.core.appmanager.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

/**
 * Updates an {@link OpenemsAppInstance}..
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppInstance",
 *   "params": {
 *     "instanceId": string (uuid),
 *     "properties": {}
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {}
 * }
 * </pre>
 */
public class UpdateAppInstance {

	public static final String METHOD = "updateAppInstance";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link UpdateAppInstance}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link UpdateAppInstance}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var instanceId = JsonUtils.getAsUUID(p, "instanceId");
			var properties = JsonUtils.getAsJsonObject(p, "properties");
			return new Request(r, instanceId, properties);
		}

		public final UUID instanceId;
		public final JsonObject properties;

		public Request(UUID instanceId, JsonObject properties) {
			super(METHOD);
			this.instanceId = instanceId;
			this.properties = properties;
		}

		private Request(JsonrpcRequest request, UUID instanceId, JsonObject properties) {
			super(request, METHOD);
			this.instanceId = instanceId;
			this.properties = properties;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("instanceId", this.instanceId.toString()) //
					.add("properties", this.properties) //
					.build();
		}
	}

}
