package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannel.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannel.Response;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelsOfComponent.ChannelRecord;

public class GetChannel implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getChannel";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String componentId, //
			String channelId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("componentId"), //
							json.getString("channelId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.addProperty("channelId", obj.channelId()) //
							.build());
		}

	}

	public record Response(ChannelRecord channel) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetChannel.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetChannel.Response> serializer() {
			return jsonObjectSerializer(GetChannel.Response.class, json -> {
				return new Response(json.getElement("channel", ChannelRecord.serializer()));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("channel", ChannelRecord.serializer().serialize(obj.channel())) //
						.build();
			});
		}

	}

}