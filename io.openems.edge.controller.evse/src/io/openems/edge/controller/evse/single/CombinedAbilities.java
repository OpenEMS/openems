package io.openems.edge.controller.evse.single;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.evse.single.Utils.combineAbilities;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

/**
 * Declares the Abilities of an {@link EvseChargePoint},
 * {@link EvseElectricVehicle} and {@link ControllerEvseSingle}.
 */
public record CombinedAbilities(//
		ChargePointAbilities chargePointAbilities, ElectricVehicleAbilities electricVehicleAbilities, //
		boolean isReadyForCharging, ApplySetPoint.Ability.Watt applySetPoint, PhaseSwitch phaseSwitch) {

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
			final var phaseSwitch = this.electricVehicleAbilities != null
					&& this.electricVehicleAbilities.canInterrupt() //
							? this.chargePointAbilities.phaseSwitch() //
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
					json.getOptionalEnum("phaseSwitch", PhaseSwitch.class).orElse(null));
		}, obj -> {
			return buildJsonObject() //
					.add("chargePointAbilities", ChargePointAbilities.serializer().serialize(obj.chargePointAbilities)) //
					.add("electricVehicleAbilities",
							ElectricVehicleAbilities.serializer().serialize(obj.electricVehicleAbilities)) //
					.addProperty("isReadyForCharging", obj.isReadyForCharging) //
					.add("applySetPoint", ApplySetPoint.Ability.serializer().serialize(obj.applySetPoint)) //
					.addProperty("phaseSwitch", obj.phaseSwitch) //
					.build();
		});
	}
}
