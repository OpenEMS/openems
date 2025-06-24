package io.openems.edge.edge2edge.websocket.bridge.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.edge2edge.websocket.bridge.jsonrpc.AuthenticateToRemoteEdge.Request;

public class AuthenticateToRemoteEdge implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "authenticateToRemoteEdge";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(String setupKey) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link AuthenticateToRemoteEdge.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<AuthenticateToRemoteEdge.Request> serializer() {
			return jsonObjectSerializer(AuthenticateToRemoteEdge.Request.class, json -> {
				return new AuthenticateToRemoteEdge.Request(json.getString("setupKey"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("setupKey", obj.setupKey()) //
						.build();
			});
		}

	}

}
