package io.openems.edge.core.appmanager.jsonrpc;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

/**
 * Gets the available {@link OpenemsApp}s.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApps",
 *   "params": {}
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
 *     apps: [{
 *       "category": string (OpenemsAppCategory enum),
 *       "appId": string,
 *       "name": string,
 *       "image": string (base64),
 *       "instanceIds": UUID[],
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetApps {

	public static final String METHOD = "getAppInstances";

	public static class Request extends JsonrpcRequest {
		public static final String METHOD = "getApps";

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link Request}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link GetAppsRequest}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsException {
			return new Request(r);
		}

		public Request() {
			super(METHOD);
		}

		private Request(JsonrpcRequest request) {
			super(request, METHOD);
		}

		@Override
		public JsonObject getParams() {
			return new JsonObject();
		}

	}

	public static class Response extends JsonrpcResponseSuccess {

		private final JsonArray apps;

		public Response(UUID id, List<OpenemsApp> availableApps, List<OpenemsAppInstance> instantiatedApps) {
			super(id);
			this.apps = createAppsArray(availableApps, instantiatedApps);
		}

		private static JsonArray createAppsArray(List<OpenemsApp> availableApps,
				List<OpenemsAppInstance> instantiatedApps) {
			var result = JsonUtils.buildJsonArray();
			for (var app : availableApps) {
				// Map Instantiated-Apps to Available-Apps
				var instanceIds = JsonUtils.buildJsonArray();
				for (var instantiatedApp : instantiatedApps) {
					if (app.getAppId().equals(instantiatedApp.appId)) {
						instanceIds.add(instantiatedApp.instanceId.toString());
						continue;
					}
				}
				result.add(JsonUtils.buildJsonObject() //
						.addProperty("category", app.getCategory().name()) //
						.addProperty("appId", app.getAppId()) //
						.addProperty("name", app.getName()) //
						.addProperty("image", app.getImage()) //
						.add("instanceIds", instanceIds.build()) //
						.build());
			}
			return result.build();
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.add("apps", this.apps) //
					.build();
		}
	}

}
