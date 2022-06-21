package io.openems.edge.core.appmanager.jsonrpc;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.validator.Validator;

/**
 * Gets the available {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApp",
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
 *     app: {
 *       "categorys": [{
 *       	"name": string (OpenemsAppCategory enum),
 *       	"readableName": string
 *       }],
 *       "cardinality": string (OpenemsAppUsage enum),
 *       "appId": string,
 *       "name": string,
 *       "status": {
 *       	"status": string (OpenemsAppStatus enum),
 *       	"errorCompatibleMessages": string[],
 *       	"errorInstallableMessages": string[]
 *       },
 *       "image": string (base64),
 *       "instanceIds": UUID[],
 *     }
 *   }
 * }
 * </pre>
 */
public class GetApp {

	public static final String METHOD = "getApp";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link Request}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link GetAppsRequest}
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
			return new JsonObject();
		}

	}

	public static class Response extends JsonrpcResponseSuccess {

		private static JsonObject createAppObject(OpenemsApp app, List<OpenemsAppInstance> instantiatedApps,
				Language language, Validator validator) {

			var instanceIds = JsonUtils.buildJsonArray();
			for (var instantiatedApp : instantiatedApps) {
				instanceIds.add(instantiatedApp.instanceId.toString());
			}
			var categorys = JsonUtils.buildJsonArray().build();
			for (var cat : app.getCategorys()) {
				categorys.add(cat.toJsonObject(language));
			}
			return JsonUtils.buildJsonObject() //
					.add("categorys", categorys) //
					.addProperty("cardinality", app.getCardinality().name()) //
					.addProperty("appId", app.getAppId()) //
					.addProperty("name", app.getName(language)) //
					.addProperty("image", app.getImage()) //
					.add("status", validator.toJsonObject(app.getValidatorConfig(), language)) //
					.add("instanceIds", instanceIds.build()) //
					.build();
		}

		private final JsonObject app;

		public Response(UUID id, OpenemsApp app, List<OpenemsAppInstance> instantiatedApps, Language language,
				Validator validator) {
			super(id);
			this.app = createAppObject(app, instantiatedApps, language, validator);
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.add("app", this.app) //
					.build();
		}
	}

}
