package io.openems.edge.edge2edge.websocket.bridge.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.edge2edge.websocket.bridge.ConnectionState;
import io.openems.edge.edge2edge.websocket.bridge.jsonrpc.GetConnectionState.Response;

public class GetConnectionState implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getConnectionState";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(ConnectionState connectionState) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetConnectionState.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetConnectionState.Response> serializer() {
			return jsonObjectSerializer(GetConnectionState.Response.class, json -> {
				return new GetConnectionState.Response(json.getEnum("connectionState", ConnectionState.class));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("connectionState", obj.connectionState()) //
						.build();
			});
		}

	}

}
