package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant.Response;

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
 *     ... {@link AppAssistant#serializer()}
 *   }
 * }
 * </pre>
 */
public class GetAppAssistant implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getAppAssistant";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String appId //
	) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAppAssistant.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppAssistant.Request> serializer() {
			return jsonObjectSerializer(GetAppAssistant.Request.class, //
					json -> new GetAppAssistant.Request(//
							json.getString("appId")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.build());
		}

	}

	public record Response(//
			AppAssistant appAssistant //
	) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAppAssistant.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppAssistant.Response> serializer() {
			return jsonSerializer(GetAppAssistant.Response.class, //
					json -> new GetAppAssistant.Response(json.getAsObject(AppAssistant.serializer())), //
					obj -> AppAssistant.serializer().serialize(obj.appAssistant()));
		}

	}

}
