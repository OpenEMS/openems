package io.openems.edge.controller.evse.single;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Utils.combineAbilities;
import static io.openems.edge.controller.evse.single.Utils.combineOppositePhaseAbilities;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

/**
 * Declares the Abilities of an {@link EvseChargePoint},
 * {@link EvseElectricVehicle} and {@link ControllerEvseSingle}.
 */
public record CombinedAbilities(//
		ChargePointAbilities chargePointAbilities, ElectricVehicleAbilities electricVehicleAbilities, //
		boolean isReadyForCharging, ApplySetPoint.Ability.Watt applySetPoint, ApplyPhaseSwitch phaseSwitch) {

	public static final class Builder {

		private final ChargePointAbilities chargePointAbilities;
		private final ElectricVehicleAbilities electricVehicleAbilities;

		private Boolean isReadyForCharging;

		public Builder(ChargePointAbilities chargePointAbilities, ElectricVehicleAbilities electricVehicleAbilities) {
			this.chargePointAbilities = chargePointAbilities;
			this.electricVehicleAbilities = electricVehicleAbilities;
		}

		public Builder setIsReadyForCharging(Boolean isReadyForCharging) {
			this.isReadyForCharging = isReadyForCharging;
			return this;
		}

		public CombinedAbilities build() {
			final var applySetPoint = combineAbilities(this.chargePointAbilities, this.electricVehicleAbilities);
			final var isReadyForCharging = this.chargePointAbilities == null || this.electricVehicleAbilities == null //
					? false //
					: this.isReadyForCharging == null //
							? this.chargePointAbilities.isReadyForCharging() //
							: this.isReadyForCharging && this.chargePointAbilities.isReadyForCharging(); //
			final var phaseSwitch = this.electricVehicleAbilities != null && this.chargePointAbilities != null
					&& this.electricVehicleAbilities.canInterrupt() //
					&& this.chargePointAbilities.phaseSwitch() != null //
							? new ApplyPhaseSwitch(//
									this.chargePointAbilities.phaseSwitch().direction(),
									this.chargePointAbilities.phaseSwitch().ability(), //
									combineOppositePhaseAbilities(//
											this.chargePointAbilities, this.electricVehicleAbilities)) //
							: null;

			return new CombinedAbilities(this.chargePointAbilities, this.electricVehicleAbilities, isReadyForCharging,
					applySetPoint, phaseSwitch);
		}
	}

	/**
	 * Create a {@link ChargePointAbilities} builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder createFrom(ChargePointAbilities chargePointAbilities,
			ElectricVehicleAbilities electricVehicleAbilities) {
		return new Builder(chargePointAbilities, electricVehicleAbilities);
	}

	/**
	 * Gets the maximum distributable set-point in watt for the current ability set.
	 *
	 * @param isAutomaticPhaseSwitching true if automatic phase switching is enabled
	 * @return the maximum set-point in watt that may be used for distribution
	 */
	public int getDistributionMaxSetPointInWatt(boolean isAutomaticPhaseSwitching) {
		var maxSetPointInWatt = this.applySetPoint.toPower(this.applySetPoint.max());
		if (!isAutomaticPhaseSwitching) {
			// Non-automatic phase switchers: only use current phase maximum
			return maxSetPointInWatt;
		}

		// Automatic phase switchers: consider both current and opposite phase maximums
		if (this.phaseSwitch == null //
				|| this.phaseSwitch.oppositePhaseApplySetPoint() == null //
				|| this.phaseSwitch.oppositePhaseApplySetPoint()
						.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
			return maxSetPointInWatt;
		}
		return Math.max(maxSetPointInWatt, this.phaseSwitch.oppositePhaseApplySetPoint().max());
	}

	/**
	 * Returns a {@link JsonSerializer} for {@link CombinedAbilities}.
	 * 
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<CombinedAbilities> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(json -> {
			return new CombinedAbilities(//
					json.getObject("chargePointAbilities", ChargePointAbilities.serializer()), //
					json.getObject("electricVehicleAbilities", ElectricVehicleAbilities.serializer()), //
					json.getBoolean("isReadyForCharging"), //
					json.getObject("applySetPoint", ApplySetPoint.Ability.Watt.serializer()), //
					json.getObject("phaseSwitch", ApplyPhaseSwitch.serializer()) //
			);
		}, obj -> {
			return buildJsonObject() //
					.add("chargePointAbilities", ChargePointAbilities.serializer().serialize(obj.chargePointAbilities)) //
					.add("electricVehicleAbilities",
							ElectricVehicleAbilities.serializer().serialize(obj.electricVehicleAbilities)) //
					.addProperty("isReadyForCharging", obj.isReadyForCharging) //
					.add("applySetPoint", ApplySetPoint.Ability.serializer().serialize(obj.applySetPoint)) //
					.add("phaseSwitch", ApplyPhaseSwitch.serializer().serialize(obj.phaseSwitch)) //
					.build();
		});
	}
}
