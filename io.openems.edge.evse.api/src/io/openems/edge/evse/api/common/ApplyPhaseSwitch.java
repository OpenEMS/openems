package io.openems.edge.evse.api.common;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;

public record ApplyPhaseSwitch(PhaseSwitchDirection direction, PhaseSwitchAbility ability) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link ApplyPhaseSwitch}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<ApplyPhaseSwitch> serializer() {
		return jsonObjectSerializer(ApplyPhaseSwitch.class, json -> new ApplyPhaseSwitch(//
				json.getOptionalEnum("direction", PhaseSwitchDirection.class).orElse(null),
				json.getObject("phaseSwitchAbility", PhaseSwitchAbility.serializer())),
				obj -> obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.add("phaseSwitchAbility", PhaseSwitchAbility.serializer().serialize(obj.ability)) //
								.addProperty("direction", obj.direction) //
								.build());
	}

	public sealed interface PhaseSwitchAbility {

		/**
		 * Returns a {@link JsonSerializer} for a {@link PhaseSwitchAbility}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		static JsonSerializer<PhaseSwitchAbility> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<PhaseSwitchAbility>create() //
					.add(Internal.class, Internal.serializer(), Internal.class.getSimpleName()) //
					.add(Manual.class, Manual.serializer(), Manual.class.getSimpleName()) //
					.build();

			return jsonSerializer(PhaseSwitchAbility.class,
					json -> json.polymorphic(polymorphicSerializer,
							t -> t.getAsJsonObjectPath().getStringPath("class")), //
					obj -> {
						if (obj == null) {
							return JsonNull.INSTANCE;
						}
						var serialized = polymorphicSerializer.serialize(obj).getAsJsonObject();
						serialized.addProperty("class", obj.getClass().getSimpleName());
						return serialized;
					});
		}

		record Internal() implements PhaseSwitchAbility {

			public static final int SWITCH_POWER_PER_PHASE = 2300;

			/**
			 * Returns a {@link JsonSerializer} for {@link Internal}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Internal> serializer() {
				return jsonObjectSerializer(//
						json -> new Internal(), //
						obj -> buildJsonObject() //
								.build());
			}
		}

		record Manual() implements PhaseSwitchAbility {

			/**
			 * Returns a {@link JsonSerializer} for {@link Manual}.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Manual> serializer() {
				return jsonObjectSerializer(//
						json -> new Manual(), //
						obj -> buildJsonObject() //
								.build());
			}
		}
	}

	public enum PhaseSwitchDirection {
		TO_SINGLE_PHASE, //
		TO_THREE_PHASE, //
	}
}
