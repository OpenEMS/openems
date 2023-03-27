package io.openems.edge.core.appmanager.jsonrpc;

import java.util.UUID;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

/**
 * Gets the active instances of an {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppInstances",
 *   "params": {
 *   	"appId": string
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
 *     "instances": {@link OpenemsAppInstance#toJsonObject()}[]
 *   }
 * }
 * </pre>
 */
public class GetAppInstances {

	public static final String METHOD = "getAppInstances";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link GetAppInstances}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link GetAppInstances}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var appId = JsonUtils.getAsString(p, "appId");
			return new Request(r, appId);
		}

		public final String appId;

		private Request(JsonrpcRequest request, String appId) {
			super(request, METHOD);
			this.appId = appId;
		}

		public Request(String appId) {
			super(METHOD);
			this.appId = appId;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("appId", this.appId) //
					.build();
		}
	}

	public static class Response extends JsonrpcResponseSuccess {

		private final JsonArray instances;

		public Response(UUID id, Stream<OpenemsAppInstance> instances) {
			super(id);

			this.instances = instances.map(OpenemsAppInstance::toJsonObject) //
					.collect(JsonUtils.toJsonArray());
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.add("instances", this.instances) //
					.build(); //
		}

	}

}
