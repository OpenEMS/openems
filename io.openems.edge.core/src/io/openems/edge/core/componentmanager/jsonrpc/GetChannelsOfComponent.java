package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.Collections.emptyList;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelsOfComponent.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelsOfComponent.Response;

public class GetChannelsOfComponent implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getChannelsOfComponent";
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

	public record Response(List<ChannelRecord> channels) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetChannelsOfComponent.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetChannelsOfComponent.Response> serializer() {
			return jsonObjectSerializer(GetChannelsOfComponent.Response.class, json -> {
				// TODO
				return new Response(emptyList());
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("channels", ChannelRecord.serializer().toListSerializer().serialize(obj.channels()))
						.build();
			});
		}

	}

	public record ChannelRecord(//
			String id, //
			AccessMode accessMode, //
			PersistencePriority persistencePriority, //
			String text, //
			OpenemsType type, //
			Unit unit, //

			ChannelCategory category, //

			// category = ChannelCategory.STATE
			Level level, //

			// category = ChannelCategory.ENUM
			List<OptionsEnum> options //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetChannelsOfComponent.ChannelRecord}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetChannelsOfComponent.ChannelRecord> serializer() {
			return jsonObjectSerializer(GetChannelsOfComponent.ChannelRecord.class, json -> {

				return null; // new GetChannelsOfComponent.ChannelRecord();
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.addProperty("accessMode", obj.accessMode().getAbbreviation()) //
						.addProperty("persistencePriority", obj.persistencePriority()) //
						.addProperty("text", obj.text()) //
						.addProperty("type", obj.type()) //
						.addProperty("unit", obj.unit().symbol) //
						.addProperty("category", obj.category()) //
						.onlyIf(obj.level() != null, t -> t.addProperty("level", obj.level())) //
						.onlyIf(obj.options() != null, t -> t.add("options", obj.options().stream() //
								.collect(toJsonObject(i -> i.getName(), i -> new JsonPrimitive(i.getValue()))))) //
						.build();
			});
		}

	}

}