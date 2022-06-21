package io.openems.edge.core.appmanager.jsonrpc;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
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
 *   "result": {
 *   	"warnings": string[]
 *   }
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

	public static class Response extends JsonrpcResponseSuccess {

		private final JsonArray warnings;

		public Response(UUID id, List<String> warnings) {
			super(id);
			this.warnings = warnings == null ? new JsonArray()
					: warnings.stream().map(JsonPrimitive::new).collect(JsonUtils.toJsonArray());
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.add("warnings", this.warnings) //
					.build();
		}
	}

}
