package io.openems.edge.evse.api.chargepoint;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;

import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.evse.api.common.ApplySetPoint;

public final class Profile {

	private Profile() {
	}

	/**
	 * Declares the Abilities of an {@link EvseChargePoint}.
	 */
	public static record ChargePointAbilities(ApplySetPoint.Ability applySetPoint, PhaseSwitch phaseSwitch,
			boolean isEvConnected, boolean isReadyForCharging) {

		public static final class Builder {

			private ApplySetPoint.Ability applySetPoint = EMPTY_APPLY_SET_POINT_ABILITY;
			private PhaseSwitch phaseSwitch = null;
			private boolean isEvConnected = false;
			private boolean isReadyForCharging = false;

			/**
			 * Defines the {@link ApplySetPoint.Ability}.
			 * 
			 * @param ability the ability
			 * @return the {@link Builder}
			 */
			public Builder setApplySetPoint(ApplySetPoint.Ability ability) {
				if (ability == null) {
					ability = EMPTY_APPLY_SET_POINT_ABILITY;
				}
				this.applySetPoint = ability;
				return this;
			}

			/**
			 * Defines the EV-Connected state.
			 * 
			 * @param isEvConnected the state
			 * @return the {@link Builder}
			 */
			public Builder setIsEvConnected(boolean isEvConnected) {
				this.isEvConnected = isEvConnected;
				return this;
			}

			/**
			 * Defines the Ready-For-Charging state.
			 * 
			 * @param isReadyForCharging the state
			 * @return the {@link Builder}
			 */
			public Builder setIsReadyForCharging(boolean isReadyForCharging) {
				this.isReadyForCharging = isReadyForCharging;
				return this;
			}

			/**
			 * Defines the {@link Profile.PhaseSwitch} Ability.
			 * 
			 * @param phaseSwitch the ability
			 * @return the {@link Builder}
			 */
			public Builder setPhaseSwitch(PhaseSwitch phaseSwitch) {
				this.phaseSwitch = phaseSwitch;
				return this;
			}

			public ChargePointAbilities build() {
				return new ChargePointAbilities(this.applySetPoint, this.phaseSwitch, this.isEvConnected,
						this.isReadyForCharging);
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link ChargeParams}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ChargePointAbilities> serializer() {
			return jsonObjectSerializer(ChargePointAbilities.class, json -> {
				return new ChargePointAbilities(//
						json.getObject("applySetPoint", ApplySetPoint.Ability.serializer()), //
						json.getEnumOrNull("phaseSwitch", PhaseSwitch.class), //
						json.getBoolean("isEvConnected"), //
						json.getBoolean("isReadyForCharging"));
			}, obj -> {
				return obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.add("applySetPoint", ApplySetPoint.Ability.serializer().serialize(obj.applySetPoint)) //
								.addProperty("phaseSwitch", obj.phaseSwitch) //
								.addProperty("isEvConnected", obj.isEvConnected) //
								.addProperty("isReadyForCharging", obj.isReadyForCharging) //
								.build();
			});
		}

		/**
		 * Create a {@link ChargePointAbilities} builder.
		 *
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}
	}

	/**
	 * Declares the Actions for an {@link EvseChargePoint}.
	 */
	public static record ChargePointActions(ChargePointAbilities abilities, ApplySetPoint.Action applySetPoint,
			PhaseSwitch phaseSwitch) {

		/**
		 * Gets the {@link ApplySetPoint} in [A].
		 * 
		 * @return the object
		 */
		public ApplySetPoint.Action.Ampere getApplySetPointInAmpere() {
			if (this.applySetPoint instanceof ApplySetPoint.Action.Ampere a) {
				return a;
			}
			throw new IllegalArgumentException(
					"ApplySetPoint is of invalid type [" + this.applySetPoint.getClass().getSimpleName() + "]");
		}

		/**
		 * Gets the {@link ApplySetPoint} in [mA].
		 * 
		 * @return the object
		 */
		public ApplySetPoint.Action.MilliAmpere getApplySetPointInMilliAmpere() {
			if (this.applySetPoint instanceof ApplySetPoint.Action.MilliAmpere ma) {
				return ma;
			}
			throw new IllegalArgumentException(
					"ApplySetPoint is of invalid type [" + this.applySetPoint.getClass().getSimpleName() + "]");
		}

		/**
		 * Gets the {@link ApplySetPoint} in [W].
		 * 
		 * @return the object
		 */
		public ApplySetPoint.Action.Watt getApplySetPointInWatt() {
			if (this.applySetPoint instanceof ApplySetPoint.Action.Watt w) {
				return w;
			}
			throw new IllegalArgumentException(
					"ApplySetPoint is of invalid type [" + this.applySetPoint.getClass().getSimpleName() + "]");
		}

		public static final class Builder {

			private final ChargePointAbilities abilities;
			private ApplySetPoint.Action applySetPoint = null;
			private PhaseSwitch phaseSwitch = null;

			private Builder(ChargePointAbilities abilities) {
				this.abilities = abilities;
			}

			private Builder(ChargePointActions actions) {
				this(actions.abilities);
				this.applySetPoint = actions.applySetPoint;
				this.phaseSwitch = actions.phaseSwitch;
			}

			public Builder setApplySetPointInMilliAmpere(int value) throws IllegalArgumentException {
				return this.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(value));
			}

			public Builder setApplySetPointInAmpere(int value) throws IllegalArgumentException {
				return this.setApplySetPoint(new ApplySetPoint.Action.Ampere(value));
			}

			public Builder setApplySetPointInWatt(int value) throws IllegalArgumentException {
				return this.setApplySetPoint(new ApplySetPoint.Action.Watt(value));
			}

			public Builder setApplyZeroSetPoint() throws IllegalArgumentException {
				return this.setApplySetPoint(switch (this.abilities.applySetPoint) {
				case ApplySetPoint.Ability.MilliAmpere ma -> new ApplySetPoint.Action.MilliAmpere(0);
				case ApplySetPoint.Ability.Ampere a -> new ApplySetPoint.Action.Ampere(0);
				case ApplySetPoint.Ability.Watt w -> new ApplySetPoint.Action.Watt(0);
				});
			}

			public Builder setApplyMinSetPoint() throws IllegalArgumentException {
				final var min = this.abilities.applySetPoint.min();
				return this.setApplySetPoint(switch (this.abilities.applySetPoint) {
				case ApplySetPoint.Ability.MilliAmpere ma -> new ApplySetPoint.Action.MilliAmpere(min);
				case ApplySetPoint.Ability.Ampere a -> new ApplySetPoint.Action.Ampere(min);
				case ApplySetPoint.Ability.Watt w -> new ApplySetPoint.Action.Watt(min);
				});
			}

			public Builder setApplySetPoint(ApplySetPoint.Action applySetPoint) throws IllegalArgumentException {
				this.applySetPoint = switch (applySetPoint) {
				case ApplySetPoint.Action.MilliAmpere ma when this.abilities.applySetPoint instanceof ApplySetPoint.Ability.MilliAmpere //
					-> ma;
				case ApplySetPoint.Action.Ampere a when this.abilities.applySetPoint instanceof ApplySetPoint.Ability.Ampere //
					-> a;
				case ApplySetPoint.Action.Watt w when this.abilities.applySetPoint instanceof ApplySetPoint.Ability.Watt //
					-> w;
				case null, default -> throw new IllegalArgumentException(
						"ApplySetPoint action must be of type [" + this.abilities.applySetPoint + "]");
				};
				return this;
			}

			public ApplySetPoint.Action getApplySetPoint() {
				return this.applySetPoint;
			}

			public Builder setPhaseSwitch(PhaseSwitch phaseSwitch) {
				if (phaseSwitch != null && phaseSwitch != this.abilities.phaseSwitch) {
					var ability = this.abilities.phaseSwitch == null //
							? "UNDEFINED" //
							: this.abilities.phaseSwitch.name();
					throw new IllegalArgumentException("PhaseSwitch not possible. " //
							+ "Ability [" + ability + "] " //
							+ "Actual [" + phaseSwitch + "]");
				}
				this.phaseSwitch = phaseSwitch;
				return this;
			}

			public ChargePointActions build() throws IllegalArgumentException {
				if (this.applySetPoint == null) {
					throw new IllegalArgumentException("ApplySetPoint is always required");
				}
				return new ChargePointActions(this.abilities, this.applySetPoint, this.phaseSwitch);
			}
		}

		/**
		 * Factory.
		 *
		 * @param abilities the {@link ChargePointAbilities}
		 * @return a {@link Builder}
		 */
		public static Builder from(ChargePointAbilities abilities) {
			return new Builder(abilities);
		}

		/**
		 * Factory.
		 *
		 * @param actions the {@link ChargePointActions}
		 * @return a {@link Builder}
		 */
		public static Builder copy(ChargePointActions actions) {
			return new Builder(actions);
		}
	}

	/**
	 * Different types of applying a phase-switch.
	 */
	public static enum PhaseSwitch { // TODO evaluate NOT_AVAILABLE instead of null
		TO_SINGLE_PHASE, //
		TO_THREE_PHASE;
	}
}
