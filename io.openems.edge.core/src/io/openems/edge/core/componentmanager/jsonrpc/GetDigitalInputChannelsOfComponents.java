package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.common.type.Tuple;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelsOfComponent.ChannelRecord;
import io.openems.edge.core.componentmanager.jsonrpc.GetDigitalInputChannelsOfComponents.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetDigitalInputChannelsOfComponents.Response;

public class GetDigitalInputChannelsOfComponents implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getDigitalInputChannelsOfComponents";
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
			List<String> componentIds //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getList("componentIds", JsonElementPath::getAsString)), //
					obj -> JsonUtils.buildJsonObject() //
							.add("componentIds", obj.componentIds().stream() //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.build());
		}

	}

	public record Response(Map<String, List<ChannelRecord>> channelsPerComponent) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetDigitalInputChannelsOfComponents.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetDigitalInputChannelsOfComponents.Response> serializer() {
			return jsonObjectSerializer(GetDigitalInputChannelsOfComponents.Response.class, json -> {
				final var resultMap = json.getList("channelsPerComponent", t -> {
					final var element = t.getAsJsonObjectPath();
					return Tuple.of(element.getString("componentId"),
							element.getList("channels", ChannelRecord.serializer()));
				}).stream().collect(toMap(Tuple::a, Tuple::b));

				return new Response(resultMap);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("channelsPerComponent", obj.channelsPerComponent().entrySet().stream() //
								.map(t -> JsonUtils.buildJsonObject() //
										.addProperty("componentId", t.getKey()) //
										.add("channels", t.getValue().stream() //
												.map(ChannelRecord.serializer()::serialize) //
												.collect(toJsonArray()))
										.build()) //
								.collect(toJsonArray())) //
						.build();
			});
		}

	}

}