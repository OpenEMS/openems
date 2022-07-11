package io.openems.edge.core.appmanager.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.OpenemsApp;

/**
 * Gets the App-Descriptor for a {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppDescriptor",
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
 *     ... {@link AppDescriptor#toJsonObject()}
 *   }
 * }
 * </pre>
 */
public class GetAppDescriptor {

	public static final String METHOD = "getAppDescriptor";

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

		private final AppDescriptor appDescriptor;

		public Response(UUID id, AppDescriptor appDescriptor) {
			super(id);
			this.appDescriptor = appDescriptor;
		}

		@Override
		public JsonObject getResult() {
			return this.appDescriptor.toJsonObject();
		}
	}

}
