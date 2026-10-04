package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;

import java.time.Clock;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.evse.api.common.ApplySetPoint;

/**
 * Parameters of one Evse.Controller.Single. Contains configuration settings,
 * runtime parameters and CombinedAbilities of Charge-Point and
 * Electric-Vehicle.
 */
public record Params(//
		/**
		 * Unique Component-ID of Evse.Controller.Single.
		 */
		String ctrlSingleId,
		/**
		 * Unique Component-ID of the EvseChargePoint.
		 */
		String chargePointId,
		/**
		 * Mode configuration of Evse.Controller.Single.
		 */
		Mode mode, //
		/**
		 * The measured ActivePower; possibly null.
		 */
		Integer activePower, //
		/**
		 * The recorded Session-Energy.
		 */
		int sessionEnergy, //
		/**
		 * The configured Session-Energy Limit; possibly null.
		 */
		Integer sessionEnergyLimit, //
		/**
		 * History data
		 */
		History history, //
		/**
		 * Current state of the state machine.
		 */
		EvseSingleState state, //
		/**
		 * PhaseSwitching configuration of Evse.Controller.Single.
		 */
		PhaseSwitching phaseSwitching, //
		/**
		 * The CombinedAbilities of Charge-Point and Electric-Vehicle.
		 */
		CombinedAbilities combinedAbilities, //
		/**
		 * JSCalendar configuration.
		 */
		JSCalendar.Tasks<Payload> tasks) {

	/**
	 * Return if it's currently possible to do a phase switch.
	 * 
	 * @return true if a phase switch is currently allowed.
	 */
	public boolean canCurrentlyDoPhaseSwitch() {
		if (this.phaseSwitching() == PhaseSwitching.DISABLE) {
			return false;
		}

		final var isNotCharging = this.state == EvseSingleState.CHARGE_PAUSED
				|| this.state == EvseSingleState.CHARGE_DISABLED;

		if (!this.combinedAbilities().electricVehicleAbilities().canInterrupt() && !isNotCharging) {
			return false;
		}

		return true;
	}

	/**
	 * Creates a set point that spans from the min of 1-phase to the max of 3-phase
	 * apply set point. If the charging station does not support a phase switch or
	 * if a phase switch is currently not possible, it returns only the active apply
	 * set point.
	 * 
	 * @return Apply set point
	 */
	public ApplySetPoint.Ability.Watt createApplySetPointForAllPhases() {
		final var currentPhaseSetPoint = this.combinedAbilities().applySetPoint();
		final var oppositePhaseSetPoint = this.getOppositePhaseSwitchApplySetPoint();

		if (this.canCurrentlyDoPhaseSwitch() && this.phaseSwitching() == PhaseSwitching.AUTOMATIC
				&& oppositePhaseSetPoint != null) {
			return new ApplySetPoint.Ability.Watt(//
					null, //
					Math.min(currentPhaseSetPoint.min(), oppositePhaseSetPoint.min()), //
					Math.max(currentPhaseSetPoint.max(), oppositePhaseSetPoint.max()), //
					currentPhaseSetPoint.step());
		}

		return currentPhaseSetPoint;
	}

	private ApplySetPoint.Ability.Watt getOppositePhaseSwitchApplySetPoint() {
		if (this.combinedAbilities().phaseSwitch() == null) {
			return null;
		}

		final var oppositeApplySetPoint = this.combinedAbilities().phaseSwitch().oppositePhaseApplySetPoint();
		if (oppositeApplySetPoint == null || oppositeApplySetPoint.equals(EMPTY_APPLY_SET_POINT_ABILITY)) {
			return null;
		}
		return oppositeApplySetPoint;
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link EshConfig}.
	 *
	 * @param clock the {@link Clock}
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Params> serializer(Clock clock) {
		return jsonObjectSerializer(json -> {
			return new Params(//
					json.getString("ctrlSingleId"), //
					json.getString("chargePointId"), //
					json.getEnum("mode", Mode.class), //
					json.getOptionalInt("activePower").orElse(null), //
					json.getInt("sessionEnergy"), //
					json.getOptionalInt("sessionEnergyLimit").orElse(null), //
					new History(), // TODO
					json.getOptionalEnum("state", EvseSingleState.class).orElse(null),
					json.getEnum("phaseSwitching", PhaseSwitching.class), //
					json.getObject("combinedAbilities", CombinedAbilities.serializer()), //
					json.getObject("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()))); //
		}, obj -> {
			return buildJsonObject() //
					.addProperty("ctrlSingleId", obj.ctrlSingleId) //
					.addProperty("chargePointId", obj.chargePointId) //
					.addProperty("mode", obj.mode) //
					.addProperty("activePower", obj.activePower) //
					.addProperty("sessionEnergy", obj.sessionEnergy) //
					.addProperty("sessionEnergyLimit", obj.sessionEnergyLimit) //
					.addProperty("history", "") // TODO
					.addProperty("state", obj.state) //
					.addProperty("phaseSwitching", obj.phaseSwitching) //
					.add("combinedAbilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
					.add("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()).serialize(obj.tasks)) //
					.build();
		});
	}
}
