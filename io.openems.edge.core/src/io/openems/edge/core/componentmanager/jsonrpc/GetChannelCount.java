package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelCount.Response;

public class GetChannelCount implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getChannelCount";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(long numberOfChannels) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetChannelCount.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetChannelCount.Response> serializer() {
			return jsonObjectSerializer(GetChannelCount.Response.class, json -> {
				return new Response(json.getLong("numberOfChannels"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("numberOfChannels", obj.numberOfChannels()) //
						.build();
			});
		}

	}

}