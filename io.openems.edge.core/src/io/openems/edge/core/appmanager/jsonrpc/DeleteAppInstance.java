package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance.Response;

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
 *   "method": "deleteAppInstance",
 *   "params": {
 *     "instanceId": string (uuid)
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
 *   	"warnings": string[]
 *   }
 * }
 * </pre>
 */
public final class DeleteAppInstance implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "deleteAppInstance";
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
			UUID instanceId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link DeleteAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<DeleteAppInstance.Request> serializer() {
			return jsonObjectSerializer(DeleteAppInstance.Request.class, //
					json -> new DeleteAppInstance.Request(//
							json.getStringPath("instanceId").getAsUuid()), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("instanceId", obj.instanceId().toString()) //
							.build());
		}

	}

	public record Response(//
			List<String> warnings //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link DeleteAppInstance.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<DeleteAppInstance.Response> serializer() {
			return jsonObjectSerializer(DeleteAppInstance.Response.class, //
					json -> new DeleteAppInstance.Response(//
							json.getList("warnings", JsonElementPath::getAsString)), //
					obj -> JsonUtils.buildJsonObject() //
							.add("warnings", obj.warnings().stream() //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.build());
		}

	}

}
