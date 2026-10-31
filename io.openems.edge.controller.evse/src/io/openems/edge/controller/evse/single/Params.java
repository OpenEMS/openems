package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.IntUtils.fitWithin;
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
		 * Externally requested minimum charge power in [W]; possibly null.
		 */
		Integer externalMinChargePower, //
		/**
		 * Externally requested maximum charge power in [W]; possibly null.
		 */
		Integer externalMaxChargePower, //
		/**
		 * Externally requested static charge power in [W]; possibly null.
		 */
		Integer externalStaticChargePower, //
		/**
		 * JSCalendar configuration.
		 */
		JSCalendar.Tasks<Payload> tasks) {

	public Params(String ctrlSingleId, String chargePointId, Mode mode, Integer activePower, int sessionEnergy,
			Integer sessionEnergyLimit, History history, EvseSingleState state, PhaseSwitching phaseSwitching,
			CombinedAbilities combinedAbilities, JSCalendar.Tasks<Payload> tasks) {
		this(ctrlSingleId, chargePointId, mode, activePower, sessionEnergy, sessionEnergyLimit, history, state,
				phaseSwitching, combinedAbilities, null, null, null, tasks);
	}

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
	 * Applies the external charge-power limits to an apply set-point.
	 *
	 * @param mode              the effective mode
	 * @param baseApplySetPoint the apply set-point before external limits
	 * @return the limited apply set-point
	 */
	public ApplySetPoint.Ability.Watt applyExternalSetPoint(Mode mode,
			ApplySetPoint.Ability.Watt baseApplySetPoint) {
		var min = baseApplySetPoint.min();
		var max = baseApplySetPoint.max();
		if (this.externalMaxChargePower != null) {
			max = fitWithin(min, max, this.externalMaxChargePower);
		}
		if (mode.isSurplusOrMinimum() && this.externalMinChargePower != null) {
			min = fitWithin(baseApplySetPoint.min(), max, this.externalMinChargePower);
		}
		if (this.externalStaticChargePower != null && this.externalStaticChargePower > 0) {
			final var staticChargePower = fitWithin(min, max, this.externalStaticChargePower);
			min = staticChargePower;
			max = staticChargePower;
		}

		return new ApplySetPoint.Ability.Watt(baseApplySetPoint.phase(), min, max, baseApplySetPoint.step());
	}

	/**
	 * Returns whether an external set-point disallows charging.
	 *
	 * @return true if charging is externally disabled
	 */
	public boolean isExternalChargeDisallowed() {
		return this.externalMaxChargePower != null && this.externalMaxChargePower <= 0
				|| this.externalStaticChargePower != null && this.externalStaticChargePower <= 0;
	}

	/**
	 * Returns whether an external static charge-power set-point is active.
	 *
	 * @return true if a positive static set-point is active
	 */
	public boolean hasExternalStaticChargePower() {
		return this.externalStaticChargePower != null && this.externalStaticChargePower > 0;
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
					json.getOptionalInt("externalMinChargePower").orElse(null), //
					json.getOptionalInt("externalMaxChargePower").orElse(null), //
					json.getOptionalInt("externalStaticChargePower").orElse(null), //
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
					.addProperty("externalMinChargePower", obj.externalMinChargePower) //
					.addProperty("externalMaxChargePower", obj.externalMaxChargePower) //
					.addProperty("externalStaticChargePower", obj.externalStaticChargePower) //
					.add("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()).serialize(obj.tasks)) //
					.build();
		});
	}
}
