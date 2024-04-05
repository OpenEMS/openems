package io.openems.edge.core.appmanager.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.OpenemsApp;

/**
 * Gets the App-Assistant for a {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppAssistant",
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
 *     ... {@link AppAssistant#toJsonObject()}
 *   }
 * }
 * </pre>
 */
public class GetAppAssistant {

	public static final String METHOD = "getAppAssistant";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link GetAppAssistantRequest}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link GetAppAssistantRequest}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var appId = JsonUtils.getAsString(p, "appId");
			return new Request(r, appId);
		}

		public final String appId;

		public Request(JsonrpcRequest request, String appId) {
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

		private final AppAssistant appAssistant;

		public Response(UUID id, AppAssistant appAssistant) {
			super(id);
			this.appAssistant = appAssistant;
		}

		@Override
		public JsonObject getResult() {
			return this.appAssistant.toJsonObject();
		}
	}

}
