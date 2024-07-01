package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.jsonrpc.GetAppDescriptor.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetAppDescriptor.Response;

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
 *     ... {@link AppDescriptor#serializer()}
 *   }
 * }
 * </pre>
 */
public class GetAppDescriptor implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getAppDescriptor";
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
		 * Returns a {@link JsonSerializer} for a {@link GetAppDescriptor.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppDescriptor.Request> serializer() {
			return jsonObjectSerializer(GetAppDescriptor.Request.class, //
					json -> new GetAppDescriptor.Request(//
							json.getString("appId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.build());
		}

	}

	public record Response(//
			AppDescriptor appDescriptor //
	) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAppDescriptor.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppDescriptor.Response> serializer() {
			return jsonSerializer(GetAppDescriptor.Response.class, //
					json -> new GetAppDescriptor.Response(json.getAsObject(AppDescriptor.serializer())), //
					obj -> AppDescriptor.serializer().serialize(obj.appDescriptor()));
		}

	}

}
