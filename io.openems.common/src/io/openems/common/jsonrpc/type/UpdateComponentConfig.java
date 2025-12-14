package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;
import java.util.Objects;

import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.UpdateComponentConfig.Request;
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
public class UpdateComponentConfig implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "updateComponentConfig";
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
			String componentId, //
			List<Property> properties //
	) {

		public Request {
			Objects.requireNonNull(componentId);
			Objects.requireNonNull(properties);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link UpdateComponentConfig.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<UpdateComponentConfig.Request> serializer() {
			return jsonObjectSerializer(UpdateComponentConfig.Request.class, //
					json -> new UpdateComponentConfig.Request(//
							json.getString("componentId"), //
							json.getList("properties", Property.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.add("properties", Property.serializer().toListSerializer().serialize(obj.properties())) //
							.build());
		}

	}

}
