package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.SubscribeChannelUpdate.Request;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'subscribeChannelUpdate'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "subscribeChannelUpdate",
 *   "params": {
 *     "subscribe": boolean
 *   }
 * }
 * </pre>
 */
public class SubscribeChannelUpdate implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "subscribeChannelUpdate";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(boolean subscribe) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link SubscribeChannelUpdate.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(json.getBoolean("subscribe")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("subscribe", obj.subscribe()) //
							.build());
		}

	}

}
