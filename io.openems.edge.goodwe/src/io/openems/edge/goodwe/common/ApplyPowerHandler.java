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
	public static void apply(AbstractGoodWe goodWe, boolean readOnlyMode, int setActivePower, ApplyPowerContext context)
			throws OpenemsNamedException {
		int pvProduction = TypeUtils.max(0, goodWe.calculatePvProduction());
		ApplyPowerHandler.Result apply = calculate(goodWe, readOnlyMode, setActivePower, pvProduction, context);

		IntegerWriteChannel emsPowerSetChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_SET);
		emsPowerSetChannel.setNextWriteValue(apply.emsPowerSet);
		EnumWriteChannel emsPowerModeChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_MODE);
		emsPowerModeChannel.setNextWriteValue(apply.emsPowerMode);
	}

	private static class Result {

		public final EmsPowerMode emsPowerMode;
		public final int emsPowerSet;

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
			Integer maxPowerConstraint = null;
			for (Constraint constraint : context.getConstraints()) {
				if (constraint.getRelationship() == Relationship.GREATER_OR_EQUALS && constraint.getValue().isPresent()
						&& constraint.getDescription().contains("[SetActivePowerGreaterOrEquals]")) {
					// Only consider runtime Constraints, provided by a Controller via
					// SetActivePowerGreaterOrEquals.
					minPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()),
							maxPowerConstraint);
					System.out.println("-minPowerConstraint [" + minPowerConstraint + "] Constraint: " + constraint);
				}
				if (constraint.getRelationship() == Relationship.LESS_OR_EQUALS && constraint.getValue().isPresent()
						&& constraint.getDescription().contains("[SetActivePowerLessOrEquals]")) {
					// Only consider runtime Constraints, provided by a Controller via
					// SetActivePowerLessOrEquals.
					maxPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()),
							maxPowerConstraint);
					System.out.println("-maxPowerConstraint [" + maxPowerConstraint + "] Constraint: " + constraint);
				}
			}
			System.out.println("actual setpoint [" + activePowerSetPoint + "] minPowerConstraint [" + minPowerConstraint
					+ "] maxPowerConstraint [" + maxPowerConstraint + "] surpluspower [" + goodWe.getSurplusPower()
					+ "] pvProduction [" + pvProduction + "]");

			// maxPowerConstraint: Limitierung Netzeinspeisung
			// minPowerConstraint: surplus feed-in ist aktiviert

			if (maxPowerConstraint != null && maxPowerConstraint == activePowerSetPoint) {
				System.out.println("EXPORT_AC [" + maxPowerConstraint
						+ "] MaxPowerConstraint for PV Curtail is set and equals activePowerSetPoint");
				// TODO try after a while if PV curtail is still required
				return new Result(EmsPowerMode.EXPORT_AC, maxPowerConstraint);

			} else if (minPowerConstraint != null && Objects.equals(minPowerConstraint, goodWe.getSurplusPower())) {
				System.out.println(
						"DISCHARGE_PV [0] MinPowerConstraint equals SurplusPower -> surplus feed-in is activated");
				return new Result(EmsPowerMode.DISCHARGE_PV, 0);

			} else if (activePowerSetPoint >= pvProduction) {
				// Set-Point is positive && bigger than PV-Production -> feed all PV to grid +
				// discharge battery
				System.out.println("DISCHARGE_PV [" + (activePowerSetPoint - pvProduction) + "] Set-Point ["
						+ activePowerSetPoint + "] bigger than PV [" + pvProduction + "]");
				return new Result(EmsPowerMode.DISCHARGE_PV, activePowerSetPoint - pvProduction);

			} else {
				// Set-Point is positive && less than PV-Production -> feed PV partly to grid +
				// charge battery
				System.out.println("CHARGE_BAT [" + (pvProduction - activePowerSetPoint) + "] Set-Point ["
						+ activePowerSetPoint + "] less than PV [" + pvProduction + "]");
				return new Result(EmsPowerMode.CHARGE_BAT, pvProduction - activePowerSetPoint);
			}

		} else if (activePowerSetPoint < 0) {
			System.out.println("IMPORT_AC [" + (activePowerSetPoint * -1) + "]");
			// Import from AC
			return new Result(EmsPowerMode.IMPORT_AC, activePowerSetPoint * -1);

		} else { // activePowerSetPoint == 0
			if (pvProduction == 0) {
				// Stop inverter
				System.out.println("STOPPED [0]");
				return new Result(EmsPowerMode.STOPPED, 0);
				// TODO consequences for Off-Grid?
			} else {
				System.out.println("IMPORT_AC [0]");
				return new Result(EmsPowerMode.IMPORT_AC, 0);
			}

		}
	}
}
