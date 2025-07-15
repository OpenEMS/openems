package io.openems.edge.evse.api.common;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static java.lang.Math.round;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.common.type.TypeUtils;

/**
 * Different types of applying a set-point.
 */
public final class ApplySetPoint {

	/**
	 * Min Current in [mA].
	 */
	public static final int MIN_CURRENT = 6000;
	/**
	 * Min Power in [W] in SINGLE_PHASE.
	 */
	public static final int MIN_POWER_SINGLE_PHASE = convertMilliAmpereToWatt(SINGLE_PHASE, MIN_CURRENT);
	/**
	 * Min Power in [W] in THREE_PHASE.
	 */
	public static final int MIN_POWER_THREE_PHASE = convertMilliAmpereToWatt(THREE_PHASE, MIN_CURRENT);

	/**
	 * Converts MilliAmpere [mA] to Watt [W].
	 * 
	 * @param phase   the {@link SingleThreePhase}
	 * @param current the value in [mA]
	 * @return the value in [W]
	 */
	public static int convertMilliAmpereToWatt(SingleOrThreePhase phase, int current) {
		return round(current / 1000f * phase.count * 230f);
	}

	/**
	 * Converts Ampere [A] to Watt [W].
	 * 
	 * @param phase   the {@link SingleThreePhase}
	 * @param current the current value
	 * @return the value in [W]
	 */
	public static int convertAmpereToWatt(SingleOrThreePhase phase, int current) {
		return convertMilliAmpereToWatt(phase, current * 1000);
	}

	/**
	 * Converts Watt [W] to MilliAmpere [mA].
	 * 
	 * @param phase the {@link SingleThreePhase}
	 * @param power the value in [W]
	 * @return the value in [mA]
	 */
	public static int convertWattToMilliAmpere(SingleOrThreePhase phase, int power) {
		return round(power * 1000 / phase.count / 230f);
	}

	/**
	 * Converts Watt [W] to Ampere [A].
	 * 
	 * @param phase the {@link SingleThreePhase}
	 * @param power the value in [W]
	 * @return the value in [A]
	 */
	public static int convertWattToAmpere(SingleOrThreePhase phase, int power) {
		return round(convertWattToMilliAmpere(phase, power) / 1000f);
	}

	/**
	 * Calculates the power step size in [W] for the given {@link Ability}.
	 * 
	 * @param ability the {@link Ability}
	 * @return the power in [W]
	 */
	public static int calculatePowerStep(Ability ability) {
		if (ability instanceof Ability.Watt w) {
			return w.step;
		}
		var min = ability.min();
		return ability.toPower(min + 1) - ability.toPower(min);
	}

	/**
	 * Rounds the given power value down to a value feasible for the
	 * {@link Ability}, considering the min-value and the step size.
	 * 
	 * @param ability the {@link Ability}
	 * @param power   the power in [W]
	 * @return the new power in [W]
	 */
	public static int roundDownToPowerStep(Ability ability, int power) {
		var step = calculatePowerStep(ability);
		if (step == 1) {
			return power;
		}
		var max = ability.toPower(ability.max());
		var min = ability.toPower(ability.min());
		power = TypeUtils.fitWithin(min, max, power);
		var aboveMin = power - min;
		return min + step * (aboveMin / step);
	}

	private ApplySetPoint() {
	}

	public sealed interface Ability {

		/**
		 * Empty Ability, in case there is no reliable information available. Sets min
		 * and max to zero.
		 */
		public static Watt EMPTY_APPLY_SET_POINT_ABILITY = new ApplySetPoint.Ability.Watt(THREE_PHASE, 0, 0);

		/**
		 * The minimum value.
		 * 
		 * @return the value
		 */
		public int min();

		/**
		 * The maximum value.
		 * 
		 * @return the value
		 */
		public int max();

		/**
		 * Gets the {@link SingleOrThreePhase}.
		 * 
		 * @return the value
		 */
		public SingleOrThreePhase phase();

		/**
		 * Converts Watt [W] to own unit.
		 * 
		 * @param power the value in [W]
		 * @return the value
		 */
		public int fromPower(int power);

		/**
		 * Converts own unit to Watt [W].
		 * 
		 * @param value the value
		 * @return the value in [W]
		 */
		public int toPower(int value);

		/**
		 * Fits the given value within {@link #min()} and {@link #max()}.
		 * 
		 * @param value the value
		 * @return the adjusted value
		 */
		public default int fitWithin(int value) {
			return TypeUtils.fitWithin(this.min(), this.max(), value);
		}

		public static record MilliAmpere(SingleOrThreePhase phase, int min, int max) implements ApplySetPoint.Ability {
			@Override
			public int toPower(int current) {
				return ApplySetPoint.convertMilliAmpereToWatt(this.phase(), current);
			}

			@Override
			public int fromPower(int power) {
				return ApplySetPoint.convertWattToMilliAmpere(this.phase(), power);
			}

			/**
			 * Returns a {@link JsonSerializer} for
			 * {@link ApplySetPoint.Ability.MilliAmpere}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ApplySetPoint.Ability.MilliAmpere> serializer() {
				return JsonSerializerUtil.jsonObjectSerializer(json -> {
					return new ApplySetPoint.Ability.MilliAmpere(//
							json.getEnum("phase", SingleOrThreePhase.class), //
							json.getInt("min"), //
							json.getInt("max"));
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("phase", obj.phase) //
							.addProperty("min", obj.min) //
							.addProperty("max", obj.max) //
							.build();
				});
			}
		}

		public static record Ampere(SingleOrThreePhase phase, int min, int max) implements ApplySetPoint.Ability {
			@Override
			public int toPower(int current) {
				return ApplySetPoint.convertAmpereToWatt(this.phase(), current);
			}

			@Override
			public int fromPower(int power) {
				return ApplySetPoint.convertWattToAmpere(this.phase(), power);
			}

			/**
			 * Returns a {@link JsonSerializer} for {@link ApplySetPoint.Ability.Ampere}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ApplySetPoint.Ability.Ampere> serializer() {
				return JsonSerializerUtil.jsonObjectSerializer(json -> {
					return new ApplySetPoint.Ability.Ampere(//
							json.getEnum("phase", SingleOrThreePhase.class), //
							json.getInt("min"), //
							json.getInt("max"));
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("phase", obj.phase) //
							.addProperty("min", obj.min) //
							.addProperty("max", obj.max) //
							.build();
				});
			}
		}

		public static record Watt(SingleOrThreePhase phase, int min, int max, int step)
				implements ApplySetPoint.Ability {

			public Watt(SingleOrThreePhase phase, int min, int max) {
				this(phase, min, max, 1);
			}

			@Override
			public int toPower(int power) {
				return power;
			}

			@Override
			public int fromPower(int power) {
				return power;
			}

			/**
			 * Returns a {@link JsonSerializer} for {@link ApplySetPoint.Ability.Watt}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ApplySetPoint.Ability.Watt> serializer() {
				return JsonSerializerUtil.jsonObjectSerializer(json -> {
					return new ApplySetPoint.Ability.Watt(//
							json.getEnum("phase", SingleOrThreePhase.class), //
							json.getInt("min"), //
							json.getInt("max"), //
							json.getInt("step"));
				}, obj -> {
					return buildJsonObject() //
							.addProperty("class", obj.getClass().getSimpleName()) //
							.addProperty("phase", obj.phase) //
							.addProperty("min", obj.min) //
							.addProperty("max", obj.max) //
							.addProperty("step", obj.step) //
							.build();
				});
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link ApplySetPoint.Ability}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ApplySetPoint.Ability> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<ApplySetPoint.Ability>create() //
					.add(Ampere.class, Ampere.serializer(), Ampere.class.getSimpleName()) //
					.add(MilliAmpere.class, MilliAmpere.serializer(), MilliAmpere.class.getSimpleName()) //
					.add(Watt.class, Watt.serializer(), Watt.class.getSimpleName()) //
					.build();

			return jsonSerializer(ApplySetPoint.Ability.class, json -> {
				return json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("class"));
			}, obj -> {
				if (obj == null) {
					return JsonNull.INSTANCE;
				}

				return polymorphicSerializer.serialize(obj);
			});
		}
	}

	public sealed interface Action {

		/**
		 * Gets the value.
		 * 
		 * @return the value in the unit defined by its class
		 */
		public int value();

		public static record MilliAmpere(int value) implements ApplySetPoint.Action {
		}

		public static record Ampere(int value) implements ApplySetPoint.Action {
		}

		public static record Watt(int value) implements ApplySetPoint.Action {
		}
	}
}