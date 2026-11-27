package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.core.host.NetworkConfiguration;

public class GetNetworkConfig implements EndpointRequestType<EmptyObject, GetNetworkConfig.Response> {

	@Override
	public String getMethod() {
		return "getNetworkConfig";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(NetworkConfiguration config) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SetNetworkConfig.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			final var serializer = NetworkConfiguration.serializer();
			return jsonSerializer(Response.class, json -> {
				return new Response(serializer.deserializePath(json));
			}, obj -> {
				return serializer.serialize(obj.config());
			});
		}

	}

}
