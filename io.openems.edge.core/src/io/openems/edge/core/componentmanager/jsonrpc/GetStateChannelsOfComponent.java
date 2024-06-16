package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelsOfComponent.ChannelRecord;
import io.openems.edge.core.componentmanager.jsonrpc.GetStateChannelsOfComponent.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetStateChannelsOfComponent.Response;

public class GetStateChannelsOfComponent implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getStateChannelsOfComponent";
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
			String componentId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(json.getString("componentId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.build());
		}

	}

	public record Response(//
			List<ChannelRecord> channels //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(json.getList("channels", ChannelRecord.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.add("channels", ChannelRecord.serializer().toListSerializer().serialize(obj.channels())) //
							.build());
		}

	}

}