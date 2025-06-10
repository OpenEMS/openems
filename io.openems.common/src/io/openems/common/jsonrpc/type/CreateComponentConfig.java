package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import com.google.gson.JsonElement;

import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.CreateComponentConfig.Request;
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
public class CreateComponentConfig implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "createComponentConfig";
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
			String factoryPid, //
			List<Property> properties //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link CreateComponentConfig.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<CreateComponentConfig.Request> serializer() {
			return jsonObjectSerializer(CreateComponentConfig.Request.class, //
					json -> new CreateComponentConfig.Request(//
							json.getString("factoryPid"), //
							json.getList("properties", Property.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("factoryPid", obj.factoryPid()) //
							.add("properties", Property.serializer().toListSerializer().serialize(obj.properties())) //
							.build());
		}

		public String getComponentId() {
			return this.properties().stream() //
					.filter(t -> t.getName().equals("id")) //
					.findFirst() //
					.map(Property::getValue) //
					.map(JsonElement::getAsString) //
					.orElse(null);
		}

	}

}
