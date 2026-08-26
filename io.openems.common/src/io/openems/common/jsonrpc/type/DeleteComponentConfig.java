package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Objects;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.DeleteComponentConfig.Request;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'createComponentConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "createComponentConfig",
 *   "params": {
 *     "factoryPid": string,
 *     "properties": [{
 *       "name": string,
 *       "value": any
 *     }]
 *   }
 * }
 * </pre>
 */
public class DeleteComponentConfig implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "deleteComponentConfig";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(//
			String componentId //
	) {

		public Request {
			Objects.requireNonNull(componentId);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link DeleteComponentConfig.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<DeleteComponentConfig.Request> serializer() {
			return jsonObjectSerializer(DeleteComponentConfig.Request.class, //
					json -> new DeleteComponentConfig.Request(json.getString("componentId")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.build());
		}

	}

}
