package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig.Request;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig.Response;

/**
 * Updates an {@link OpenemsAppInstance}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppConfig",
 *   "params": {
 *     "componentId": string (uuid),
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
 *     "instance": {@link OpenemsAppInstance#toJsonObject()}
 *     "warnings": string[]
 *   }
 * }
 * </pre>
 */
public class UpdateAppConfig implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "updateAppConfig";
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
			String componentId, //
			JsonObject properties //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link UpdateAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UpdateAppConfig.Request> serializer() {
			return jsonObjectSerializer(UpdateAppConfig.Request.class, //
					json -> new UpdateAppConfig.Request(//
							json.getString("componentId"), //
							json.getJsonObject("properties")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.add("properties", obj.properties()) //
							.build());
		}

	}

	public record Response(//
	
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link UpdateAppInstance.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UpdateAppConfig.Response> serializer() {
			return JsonSerializerUtil.emptyObjectSerializer(Response::new);
		}

	}

}
