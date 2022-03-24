package io.openems.edge.core.appmanager.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

/**
 * Adds an {@link OpenemsAppInstance}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "addAppInstance",
 *   "params": {
 *     "appId": string,
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
 *   "result": {
 *     "instanceId": string (uuid)
 *   }
 * }
 * </pre>
 */
public class AddAppInstance {

	public static final String METHOD = "addAppInstance";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link AddAppInstance}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link AddAppInstance} Request
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var appId = JsonUtils.getAsString(p, "appId");
			var alias = JsonUtils.getAsString(p, "alias");
			var properties = JsonUtils.getAsJsonObject(p, "properties");
			return new Request(r, appId, alias, properties);
		}

		public final String appId;
		public final String alias;
		public final JsonObject properties;

		private Request(JsonrpcRequest request, String appId, String alias, JsonObject properties) {
			super(request, METHOD);
			this.appId = appId;
			this.alias = alias;
			this.properties = properties;
		}

		public Request(String appId, String alias, JsonObject properties) {
			super(METHOD);
			this.appId = appId;
			this.alias = alias;
			this.properties = properties;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("appId", this.appId) //
					.addProperty("alias", this.alias) //
					.add("properties", this.properties) //
					.build();
		}
	}

	public static class Response extends JsonrpcResponseSuccess {

		private final UUID instanceId;

		public Response(UUID id, UUID instanceId) {
			super(id);
			this.instanceId = instanceId;
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.addProperty("instanceId", this.instanceId.toString()) //
					.build();
		}
	}

}
