package io.openems.edge.evse.api.electricvehicle;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplySetPoint;

public final class Profile {

	private Profile() {
	}

	/**
	 * Declares the Abilities of an {@link EvseElectricVehicle}.
	 */
	public static record ElectricVehicleAbilities(//
			ApplySetPoint.Ability.Watt singlePhaseLimit, //
			ApplySetPoint.Ability.Watt threePhaseLimit, //
			boolean canInterrupt) {

		public static final class Builder {

			private ApplySetPoint.Ability.Watt singlePhaseLimit = EMPTY_APPLY_SET_POINT_ABILITY;
			private ApplySetPoint.Ability.Watt threePhaseLimit = EMPTY_APPLY_SET_POINT_ABILITY;

			/**
			 * EV does not support interrupting a charging session. Instead charge current
			 * is reduced to minimum by the Controller in this case.
			 */
			private boolean canInterrupt = false;

			/**
			 * Sets the limit for {@link SingleThreePhase#SINGLE_PHASE}.
			 * 
			 * @param min minimum power in [W]
			 * @param max maximum power in [W]
			 * @return the {@link Builder}
			 */
			public Builder setSinglePhaseLimitInWatt(int min, int max) {
				this.singlePhaseLimit = new ApplySetPoint.Ability.Watt(SINGLE_PHASE, min, max);
				return this;
			}

			/**
			 * Sets the limit for {@link SingleThreePhase#THREE_PHASE}.
			 * 
			 * @param min minimum current in [mA]
			 * @param max maximum current in [mA]
			 * @return the {@link Builder}
			 */
			public Builder setSinglePhaseLimitInMilliAmpere(int min, int max) {
				return this.setSinglePhaseLimitInWatt(//
						convertMilliAmpereToWatt(SINGLE_PHASE, min), //
						convertMilliAmpereToWatt(SINGLE_PHASE, max));
			}

			/**
			 * Sets the limit for {@link SingleThreePhase#THREE_PHASE}.
			 * 
			 * @param min minimum power in [W]
			 * @param max maximum power in [W]
			 * @return the {@link Builder}
			 */
			public Builder setThreePhaseLimitInWatt(int min, int max) {
				this.threePhaseLimit = new ApplySetPoint.Ability.Watt(THREE_PHASE, min, max);
				return this;
			}

			/**
			 * Sets the limit for {@link SingleThreePhase#THREE_PHASE}.
			 * 
			 * @param min minimum current in [mA]
			 * @param max maximum current in [mA]
			 * @return the {@link Builder}
			 */
			public Builder setThreePhaseLimitInMilliAmpere(int min, int max) {
				return this.setThreePhaseLimitInWatt(//
						convertMilliAmpereToWatt(THREE_PHASE, min), //
						convertMilliAmpereToWatt(THREE_PHASE, max));
			}

			/**
			 * Defines this EV to be interruptable.
			 * 
			 * @param canInterrupt canInterrupt
			 * @return the {@link Builder}
			 */
			public Builder setCanInterrupt(boolean canInterrupt) {
				this.canInterrupt = canInterrupt;
				return this;
			}

			public ElectricVehicleAbilities build() {
				return new ElectricVehicleAbilities(this.singlePhaseLimit, this.threePhaseLimit, this.canInterrupt);
			}
		}

		/**
		 * Create a {@link ChargePointAbilities} builder.
		 *
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Returns a {@link JsonSerializer} for {@link CombinedAbilities}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ElectricVehicleAbilities> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(json -> {
				return new ElectricVehicleAbilities(//
						json.getObject("singlePhaseLimit", ApplySetPoint.Ability.Watt.serializer()), //
						json.getObject("threePhaseLimit", ApplySetPoint.Ability.Watt.serializer()), //
						json.getBoolean("canInterrupt"));
			}, obj -> {
				return buildJsonObject() //
						.add("singlePhaseLimit", //
								ApplySetPoint.Ability.Watt.serializer().serialize(obj.singlePhaseLimit)) //
						.add("threePhaseLimit", //
								ApplySetPoint.Ability.Watt.serializer().serialize(obj.threePhaseLimit)) //
						.addProperty("canInterrupt", obj.canInterrupt) //
						.build();
			});
		}
	}
}
