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
 *   "method": "deleteAppInstance",
 *   "params": {
 *     "instanceId": string (uuid)
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
public class DeleteAppInstance {

	public static final String METHOD = "deleteAppInstance";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link DeleteAppInstance}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link DeleteAppInstance}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var instanceId = JsonUtils.getAsUUID(p, "instanceId");
			return new Request(r, instanceId);
		}

		public final UUID instanceId;

		private Request(JsonrpcRequest request, UUID instanceId) {
			super(request, METHOD);
			this.instanceId = instanceId;
		}

		public Request(UUID instanceId) {
			super(METHOD);
			this.instanceId = instanceId;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("instanceId", this.instanceId.toString()) //
					.build();
		}
	}

}
