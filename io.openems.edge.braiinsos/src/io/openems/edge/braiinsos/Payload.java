package io.openems.edge.braiinsos;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;

public sealed interface Payload {

	record Manual(Mode mode) implements Payload {

		/**
		 * Returns a {@link JsonSerializer} for {@link Payload.Manual}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Manual> serializer() {
			return jsonObjectSerializer(json -> {
				return new Manual(//
						json.getEnum("mode", Mode.class));
			}, obj -> {
				return buildJsonObject()//
						.addProperty("class", obj.getClass().getSimpleName())//
						.addProperty("mode", obj.mode)//
						.build();
			});
		}
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link Payload}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	static JsonSerializer<Payload> serializer() {
		final var polymorphicSerializer = PolymorphicSerializer.<Payload>create() //
				.add(Manual.class, Manual.serializer(), Manual.class.getSimpleName()) //
				.build();

		return jsonSerializer(Payload.class, json -> {
			return json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("class"));
		}, obj -> {
			return polymorphicSerializer.serialize(obj);
		});
	}
}
