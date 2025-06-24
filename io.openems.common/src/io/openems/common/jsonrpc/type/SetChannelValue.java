package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Objects;

import com.google.gson.JsonElement;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.SetChannelValue.Request;
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
public class SetChannelValue implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "setChannelValue";
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
			String channelId, //
			JsonElement value //
	) {

		public Request {
			Objects.requireNonNull(componentId);
			Objects.requireNonNull(channelId);
			Objects.requireNonNull(value);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link SetChannelValue.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<SetChannelValue.Request> serializer() {
			return jsonObjectSerializer(SetChannelValue.Request.class, //
					json -> new SetChannelValue.Request(//
							json.getString("componentId"), //
							json.getString("channelId"), //
							json.getJsonElement("value")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.addProperty("channelId", obj.channelId()) //
							.add("value", obj.value()) //
							.build());
		}

	}

}
