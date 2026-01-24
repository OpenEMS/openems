package io.openems.edge.common.oauth.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.oauth.ConnectionState;

public class GetOAuthConnectionState
		implements EndpointRequestType<GetOAuthConnectionState.Request, GetOAuthConnectionState.Response> {

	@Override
	public String getMethod() {
		return "getOAuthConnectionState";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(String identifier) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getString("identifier") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.build();
			});
		}

	}

	public record Response(ConnectionState connectionState) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(//
						json.getEnum("connectionState", ConnectionState.class) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("connectionState", obj.connectionState()) //
						.build();
			});
		}

	}

}
