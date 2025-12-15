package io.openems.edge.common.oauth.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class ConnectOAuth implements EndpointRequestType<ConnectOAuth.Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "connect";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(String identifier, String state, String code) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getString("identifier"), //
						json.getString("state"), //
						json.getString("code") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.addProperty("state", obj.state()) //
						.addProperty("code", obj.code()) //
						.build();
			});
		}

	}

}
