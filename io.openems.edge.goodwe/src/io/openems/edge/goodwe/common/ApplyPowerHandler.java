package io.openems.edge.goodwe.common;

import java.util.Objects;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ApplyPowerContext;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;

public class ApplyPowerHandler {

	private final static int awaitingHysteresisForEmsPowerModeChange = 10;
//	private final static int MIN_WATCH_PERIOD = 2; // seconds
//	private final static int MAX_WATCH_PERIOD = 100; // seconds
//	private int watchPeriodForEmsPowerModeChanges = 10; // seconds
//	private final TreeSet<Instant> emsPowerModeChanges = new TreeSet<>();

//
	private EmsPowerMode lastEmsPowerMode = EmsPowerMode.UNDEFINED;
	private int changeEmsPowerModeCounter = awaitingHysteresisForEmsPowerModeChange;
//	private Instant lastChangeEmsPowerMode = Instant.MIN;

	/**
	 * Apply the desired Active-Power Set-Point by setting the appropriate
	 * EMS_POWER_SET and EMS_POWER_MODE settings.
	 * 
	 * @param goodWe         the GoodWe - either Battery-Inverter or ESS
	 * @param readOnlyMode   is Read-Only-Mode activated?
	 * @param setActivePower the Active-Power Set-Point
	 * @param context        the {@link ApplyPowerContext}
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(AbstractGoodWe goodWe, boolean readOnlyMode, int setActivePower,
			ApplyPowerContext context) throws OpenemsNamedException {
		int pvProduction = TypeUtils.max(0, goodWe.calculatePvProduction());
		ApplyPowerHandler.Result apply = calculate(goodWe, readOnlyMode, setActivePower, pvProduction, context);

		// TODO new logic for ems power mode change
//		if (this.lastEmsPowerMode == apply.emsPowerMode) {
//			// no change in Set-Power-Mode
//		} else {
//			// change in Set-Power-Mode
//			Instant now = Instant.now();
//			this.emsPowerModeChanges
//					.removeIf(instant -> instant.isBefore(now.minusSeconds(this.watchPeriodForEmsPowerModeChanges)));
//			if (this.emsPowerModeChanges.isEmpty()) {
//				// decrease 'watchPeriodForEmsPowerModeChanges'
//				this.watchPeriodForEmsPowerModeChanges = TypeUtils.fitWithin(MIN_WATCH_PERIOD, MAX_WATCH_PERIOD,
//						this.watchPeriodForEmsPowerModeChanges - 1);
//			} else {
//				// increase 'watchPeriodForEmsPowerModeChanges'
//				this.watchPeriodForEmsPowerModeChanges = TypeUtils.fitWithin(MIN_WATCH_PERIOD, MAX_WATCH_PERIOD,
//						this.watchPeriodForEmsPowerModeChanges + 1);
//			}
//		}

		// Do not change EMS-Power-Mode faster than CHANGE_EMS_POWER_MODE_AFTER_CYCLES
		if (this.lastEmsPowerMode == apply.emsPowerMode) {
			// Keep EMS-Power-Mode and -Set
			this.changeEmsPowerModeCounter = 0;

		} else if (++this.changeEmsPowerModeCounter >= awaitingHysteresisForEmsPowerModeChange) {
			// Apply new EMS-Power-Mode
			this.changeEmsPowerModeCounter = 0;
			System.out.println(
					"Changing EMS-Power-Mode from [" + this.lastEmsPowerMode + "] to [" + apply.emsPowerMode + "]");

		} else {
			// Keep old EMS-Power-Mode, but 'set' zero
			System.out.println("Waiting to change EMS-Power-Mode from [" + this.lastEmsPowerMode + "] to ["
					+ apply.emsPowerMode + "]: " + this.changeEmsPowerModeCounter);
			apply.emsPowerMode = this.lastEmsPowerMode;
			apply.emsPowerSet = 0;
		}
		this.lastEmsPowerMode = apply.emsPowerMode;

		// Set Channels
		IntegerWriteChannel emsPowerSetChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_SET);
		emsPowerSetChannel.setNextWriteValue(apply.emsPowerSet);
		EnumWriteChannel emsPowerModeChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_MODE);
		emsPowerModeChannel.setNextWriteValue(apply.emsPowerMode);
	}

	private static class Result {

		protected EmsPowerMode emsPowerMode;
		protected int emsPowerSet;

		public Result(EmsPowerMode emsPowerMode, int emsPowerSet) {
			this.emsPowerMode = emsPowerMode;
			this.emsPowerSet = emsPowerSet;
		}

	}

	private static ApplyPowerHandler.Result calculate(AbstractGoodWe goodWe, boolean readOnlyMode,
			int activePowerSetPoint, int pvProduction, ApplyPowerContext context) {
		if (readOnlyMode) {
			// Read-Only
			return new Result(EmsPowerMode.AUTO, 0);
		}

		if (activePowerSetPoint > 0) {
			Integer minPowerConstraint = null;
//			Integer maxPowerConstraint = null;
			for (Constraint constraint : context.getConstraints()) {
				if (constraint.getRelationship() == Relationship.GREATER_OR_EQUALS && constraint.getValue().isPresent()
						&& constraint.getDescription().contains("[SetActivePowerGreaterOrEquals]")) {
					// Only consider runtime Constraints, provided by a Controller via
					// SetActivePowerGreaterOrEquals.
					minPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()),
							minPowerConstraint);
				}

//				if (constraint.getRelationship() == Relationship.LESS_OR_EQUALS && constraint.getValue().isPresent()
//						&& constraint.getDescription().contains("[SetActivePowerLessOrEquals]")) {
//					// Only consider runtime Constraints, provided by a Controller via
//					// SetActivePowerLessOrEquals.
//					maxPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()),
//							maxPowerConstraint);
//				}
			}

			// maxPowerConstraint: Limitierung Netzeinspeisung
			// minPowerConstraint: surplus feed-in ist aktiviert

//			if (maxPowerConstraint != null && maxPowerConstraint == activePowerSetPoint) {
//				System.out.println("EXPORT_AC [" + maxPowerConstraint
//						+ "] MaxPowerConstraint for PV Curtail is set and equals activePowerSetPoint");
//				// TODO try after a while if PV curtail is still required
//				return new Result(EmsPowerMode.EXPORT_AC, maxPowerConstraint);
//
//			} else 
			if (minPowerConstraint != null && Objects.equals(minPowerConstraint, goodWe.getSurplusPower())
					&& Objects.equals(minPowerConstraint, activePowerSetPoint)) {
				System.out.println(
						"CHARGE_PV [0] MinPowerConstraint equals SurplusPower -> surplus feed-in is activated ["
								+ activePowerSetPoint + "]");
				return new Result(EmsPowerMode.CHARGE_PV, 0);

			} else if (pvProduction >= activePowerSetPoint) {
				// Set-Point is positive && less than PV-Production -> feed PV partly to grid +
				// charge battery
				System.out.println("CHARGE_BAT [" + (pvProduction - activePowerSetPoint) + "] Set-Point ["
						+ activePowerSetPoint + "] less than PV [" + pvProduction + "]");
				return new Result(EmsPowerMode.CHARGE_BAT, pvProduction - activePowerSetPoint);

			} else {
				// Set-Point is positive && bigger than PV-Production -> feed all PV to grid +
				// discharge battery
				System.out.println("DISCHARGE_BAT [" + (activePowerSetPoint - pvProduction) + "] Set-Point ["
						+ activePowerSetPoint + "] bigger than PV [" + pvProduction + "]");
				return new Result(EmsPowerMode.DISCHARGE_BAT, activePowerSetPoint - pvProduction);
			}

		} else if (activePowerSetPoint < 0) {
			// PV > Set-Point *-1?
			// (PV 4000; Set-Point -2000)
			// Charge_BAT 6000; Charge-Max-Current -1
			// PV < Set-Point * -1?
			// (PV 4000; Set-Point -5000)
			// Charge_BAT 9000; Charge-Max-Current -1
			System.out.println("CHARGE_BAT [" + (activePowerSetPoint * -1 + pvProduction) + "] Set-Point ["
					+ activePowerSetPoint + "] PV [" + pvProduction + "]");
			return new Result(EmsPowerMode.CHARGE_BAT, activePowerSetPoint * -1 + pvProduction);

		} else { // activePowerSetPoint == 0
			// if (pvProduction == 0) {
			//// Stop inverter to reduce power;
			// System.out.println("STOPPED [0]");
			// return new Result(EmsPowerMode.STOPPED, 0);
			//// TODO consequences for Off-Grid?
			// TODO disabled for now, because STOPPED also stops the PV
			// } else {

			// IMPORT_AC seems to not curtail PV...

			System.out.println("IMPORT_AC [0]");
			return new Result(EmsPowerMode.IMPORT_AC, 0);

		}
	}
}
