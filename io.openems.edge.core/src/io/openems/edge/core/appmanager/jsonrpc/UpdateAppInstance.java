package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance.Response;

/**
 * Updates an {@link OpenemsAppInstance}..
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppInstance",
 *   "params": {
 *     "instanceId": string (uuid),
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
public class UpdateAppInstance implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "updateAppInstance";
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
			UUID instanceId, //
			String alias, //
			JsonObject properties //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link UpdateAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UpdateAppInstance.Request> serializer() {
			return jsonObjectSerializer(UpdateAppInstance.Request.class, //
					json -> new UpdateAppInstance.Request(//
							json.getStringPath("instanceId").getAsUuid(), //
							json.getString("alias"), //
							json.getJsonObject("properties")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("instanceId", obj.instanceId().toString()) //
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
		 * Returns a {@link JsonSerializer} for a {@link UpdateAppInstance.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UpdateAppInstance.Response> serializer() {
			return jsonObjectSerializer(UpdateAppInstance.Response.class, //
					json -> new UpdateAppInstance.Response(//
							json.getElement("instance", OpenemsAppInstance.serializer()), //
							json.getList("warnings", JsonElementPath::getAsString)), //
					obj -> JsonUtils.buildJsonObject() //
							.add("instance", OpenemsAppInstance.serializer().serialize(obj.instance())) //
							.add("warnings", obj.warnings().stream() //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.build());
		}

	}

}
