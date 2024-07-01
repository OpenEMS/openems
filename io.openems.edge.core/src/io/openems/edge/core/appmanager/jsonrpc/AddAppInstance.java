package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Response;

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
 *     "instance": {@link OpenemsAppInstance#toJsonObject()}
 *     "warnings": string[]
 *   }
 * }
 * </pre>
 */
public class AddAppInstance implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "addAppInstance";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public static record Request(//
			String appId, //
			String key, //
			String alias, //
			JsonObject properties //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("appId"), //
							json.getString("key"), //
							json.getString("alias"), //
							json.getJsonObject("properties")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.addProperty("key", obj.key()) //
							.addProperty("alias", obj.alias()) //
							.add("properties", obj.properties()) //
							.build());
		}

	}

	public record Response(//
			OpenemsAppInstance instance, //
			List<String> warnings //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getElement("instance", OpenemsAppInstance.serializer()), //
							json.getList("warnings", JsonElementPath::getAsString)), //
					obj -> JsonUtils.buildJsonObject() //
							.add("instance", obj.instance.toJsonObject()) //
							.add("warnings", obj.warnings.stream() //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.build());
		}
	}

}
