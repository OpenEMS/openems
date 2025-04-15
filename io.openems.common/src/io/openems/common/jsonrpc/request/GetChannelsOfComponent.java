package io.openems.common.jsonrpc.request;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent.Request;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent.Response;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.StringParser;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils;

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
			String componentId, //
			boolean requireEnabled //
	) {

		public Request(String componentId) {
			this(componentId, false);
		}

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetChannelsOfComponent.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("componentId"), //
							json.getOptionalBoolean("requireEnabled").orElse(false)), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.addProperty("requireEnabled", obj.requireEnabled()) //
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
				return new Response(json.getList("channels", ChannelRecord.serializer()));
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
			List<OptionsEnumEntry> options //
	) {

		public record OptionsEnumEntry(String name, int value) {

			/**
			 * Creates a {@link OptionsEnumEntry} from a {@link OptionsEnum}.
			 * 
			 * @param optionsEnum the {@link OptionsEnum}
			 * @return the created {@link OptionsEnumEntry}
			 */
			public static OptionsEnumEntry from(OptionsEnum optionsEnum) {
				return new OptionsEnumEntry(optionsEnum.getName(), optionsEnum.getValue());
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetChannelsOfComponent.ChannelRecord}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetChannelsOfComponent.ChannelRecord> serializer() {
			return jsonObjectSerializer(GetChannelsOfComponent.ChannelRecord.class, json -> {
				return new GetChannelsOfComponent.ChannelRecord(//
						json.getString("id"), //
						json.getObject("accessMode", accessModeSerializer()), //
						json.getEnum("persistencePriority", PersistencePriority.class), //
						json.getString("text"), //
						json.getEnum("type", OpenemsType.class), //
						json.getObject("unit", unitSerializer()), //
						json.getEnum("category", ChannelCategory.class), //
						json.getEnumOrNull("level", Level.class), //
						json.getNullableJsonObjectPath("options").mapIfPresent(o -> o.collectStringKeys(
								mapping(t -> new OptionsEnumEntry(t.getKey(), t.getValue().getAsInt()), toList()))) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.add("accessMode", accessModeSerializer().serialize(obj.accessMode())) //
						.addProperty("persistencePriority", obj.persistencePriority()) //
						.addProperty("text", obj.text()) //
						.addProperty("type", obj.type()) //
						.add("unit", unitSerializer().serialize(obj.unit())) //
						.addProperty("category", obj.category()) //
						.onlyIf(obj.level() != null, t -> t.addProperty("level", obj.level())) //
						.onlyIf(obj.options() != null, t -> t.add("options", obj.options().stream() //
								.collect(toJsonObject(OptionsEnumEntry::name, i -> new JsonPrimitive(i.value()))))) //
						.build();
			});
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AccessMode}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<AccessMode> accessModeSerializer() {
			return jsonSerializer(AccessMode.class, json -> {
				return json.getAsStringParsed(new AccessModeStringParser());
			}, obj -> {
				return new JsonPrimitive(obj.getAbbreviation());
			});
		}

		private static class AccessModeStringParser implements StringParser<AccessMode> {

			@Override
			public AccessMode parse(String value) {
				return Stream.of(AccessMode.values()) //
						.filter(t -> t.getAbbreviation().equals(value)) //
						.findAny().orElseThrow(() -> new OpenemsRuntimeException(
								"Unable to find Access mode with abbreviation " + value));
			}

			@Override
			public ExampleValues<AccessMode> getExample() {
				return new ExampleValues<>(AccessMode.READ_ONLY.getAbbreviation(), AccessMode.READ_ONLY);
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Unit}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Unit> unitSerializer() {
			return jsonSerializer(Unit.class, json -> {
				return json.getAsStringParsed(new UnitStringParser());
			}, obj -> {
				return new JsonPrimitive(obj.symbol);
			});
		}

		private static class UnitStringParser implements StringParser<Unit> {

			@Override
			public Unit parse(String value) {
				final var unit = Unit.fromSymbolOrElse(value, null);
				if (unit == null) {
					new OpenemsRuntimeException("Unable to find Unit with symbol " + value);
				}
				return unit;
			}

			@Override
			public ExampleValues<Unit> getExample() {
				return new ExampleValues<>(Unit.WATT.symbol, Unit.WATT);
			}

		}

	}

}