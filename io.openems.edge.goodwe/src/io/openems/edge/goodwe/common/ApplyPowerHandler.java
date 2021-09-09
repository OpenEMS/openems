package io.openems.edge.goodwe.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;

public class ApplyPowerHandler {

	/**
	 * Apply the desired Active-Power Set-Point by setting the appropriate
	 * EMS_POWER_SET and EMS_POWER_MODE settings.
	 * 
	 * @param goodWe          the GoodWe - either Battery-Inverter or ESS
	 * @param setActivePower  the Active-Power Set-Point
	 * @param controlMode     the {@link ControlMode} to handle the different
	 *                        {@link EmsPowerMode} for the GoodWe battery inverter
	 * @param gridActivePower the grid active power
	 * @param essActivePower  the ESS active power
	 * @param maxAcImport     the max AC import power
	 * @param maxAcExport     the max AC export power
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(AbstractGoodWe goodWe, int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower, Value<Integer> essActivePower, Value<Integer> maxAcImport,
			Value<Integer> maxAcExport) throws OpenemsNamedException {
		int pvProduction = TypeUtils.max(0, goodWe.calculatePvProduction());

		final ApplyPowerHandler.Result apply;
		if (gridActivePower.isDefined() && essActivePower.isDefined() && maxAcImport.isDefined()
				&& maxAcExport.isDefined()) {
			apply = calculate(goodWe, setActivePower, pvProduction, controlMode, gridActivePower.get(),
					essActivePower.get(), maxAcImport.get(), maxAcExport.get());
		} else {
			// If any Channel Value is not available: fall back to AUTO mode
			apply = new ApplyPowerHandler.Result(EmsPowerMode.AUTO, 0);
		}

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

	private static ApplyPowerHandler.Result calculate(AbstractGoodWe goodWe, int activePowerSetPoint, int pvProduction,
			ControlMode controlMode, int gridActivePower, int essActivePower, int maxAcImport, int maxAcExport)
			throws OpenemsNamedException {
		switch (controlMode) {
		case INTERNAL:
			return handleInternalMode();
		case SMART:
			return handleSmartMode(goodWe, activePowerSetPoint, pvProduction, gridActivePower, essActivePower,
					maxAcImport, maxAcExport);
		case REMOTE:
			return handleRemoteMode(activePowerSetPoint, pvProduction);
		default:
			return handleInternalMode();
		}
	}

	private static Result handleInternalMode() {
		return new Result(EmsPowerMode.AUTO, 0);
	}

	private static Result handleSmartMode(AbstractGoodWe goodWe, int activePowerSetPoint, int pvProduction,
			int gridActivePower, int essActivePower, int maxAcImport, int maxAcExport) throws OpenemsNamedException {

		// Is Balancing to zero active?
		int diffBalancing = activePowerSetPoint - (gridActivePower + essActivePower);

		// Is Surplus-Feed-In active?
		final Integer surplusPower = goodWe.getSurplusPower();
		int diffSurplus = Integer.MAX_VALUE;
		if (surplusPower != null) {
			diffSurplus = activePowerSetPoint - surplusPower;
		}

		// Is charging from AC at maximum?
		// PV = 10.000
		// Max AC import = 3.000
		// ActivePowerSetPoint = 3.000
		int diffMaxAcImport = activePowerSetPoint - maxAcImport;

		// Is discharging from AC at maximum?
		// PV = 0
		// Max AC import = 8.000
		// ActivePowerSetPoint = 8.000
		int diffMaxAcExport = activePowerSetPoint - maxAcExport;

		if ((diffBalancing > -1 && diffBalancing < 1) || (diffSurplus > -1 && diffSurplus < 1)
				|| (diffMaxAcImport > -1 && diffMaxAcImport < 1) || (diffMaxAcExport > -1 && diffMaxAcExport < 1)) {
			// avoid rounding errors
			return handleInternalMode();
		}

		return handleRemoteMode(activePowerSetPoint, pvProduction);
	}

	private static Result handleRemoteMode(int activePowerSetPoint, int pvProduction) {
		// TODO PV curtail: (surplus power == setpoint && battery soc == 100% => PV
		// curtail)
		if (activePowerSetPoint >= 0) {
			if (pvProduction >= activePowerSetPoint) {
				// Set-Point is positive && less than PV-Production -> feed PV partly to grid +
				// charge battery
				// On Surplus Feed-In PV == Set-Point => CHARGE_BAT 0
				return new Result(EmsPowerMode.CHARGE_BAT, pvProduction - activePowerSetPoint);

			} else {
				// Set-Point is positive && bigger than PV-Production -> feed all PV to grid +
				// discharge battery
				return new Result(EmsPowerMode.DISCHARGE_BAT, activePowerSetPoint - pvProduction);

			}
		} else {
			return new Result(EmsPowerMode.CHARGE_BAT, activePowerSetPoint * -1 + pvProduction);
		}
	}

}
