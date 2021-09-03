package io.openems.edge.goodwe.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverter;
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
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(AbstractGoodWe goodWe, int setActivePower, ControlMode controlMode,
			int gridActivePower, int essActivePower) throws OpenemsNamedException {
		int pvProduction = TypeUtils.max(0, goodWe.calculatePvProduction());
		ApplyPowerHandler.Result apply = calculate(goodWe, setActivePower, pvProduction, controlMode, gridActivePower,
				essActivePower);

		System.out.println(
				"ApplyPowerHandler emsPowerMode[" + apply.emsPowerMode + "]emsPowerSet[" + apply.emsPowerSet + "]");

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
			ControlMode controlMode, int gridActivePower, int essActivePower) throws OpenemsNamedException {
		EnumReadChannel meterCommunicationChannel = goodWe.channel(GoodWe.ChannelId.METER_COMMUNICATE_STATUS);
		Value<Integer> meterCommunication = meterCommunicationChannel.value();
		if (!meterCommunication.isDefined() || meterCommunication.get() != 1) {
			return handleRemoteMode(activePowerSetPoint, pvProduction);
		}

		switch (controlMode) {
		case INTERNAL:
			return handleInternalMode();
		case SMART:
			return handleSmartMode(goodWe, activePowerSetPoint, pvProduction, gridActivePower, essActivePower);
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
			int meterActivePower, int essActivePower) throws OpenemsNamedException {

		// Is Balancing to zero active?
		int diffBalancing = activePowerSetPoint - (meterActivePower + essActivePower);

		// Is Surplus-Feed-In active?
		final Integer surplusPower = goodWe.getSurplusPower();
		int diffSurplus = Integer.MAX_VALUE;
		if (surplusPower != null) {
			diffSurplus = activePowerSetPoint - surplusPower;
		}

		final int diffAcMaximum;
		if (goodWe instanceof GoodWeBatteryInverter) {
			// Is charging from AC at maximum?
			// PV = 10.000
			// Max AC import = 3.000
			// ActivePowerSetPoint = 3.000
			IntegerReadChannel maxAcImportChannel = goodWe.channel(GoodWeBatteryInverter.ChannelId.MAX_AC_IMPORT);
			if (maxAcImportChannel.value().isDefined()) {
				diffAcMaximum = activePowerSetPoint - maxAcImportChannel.value().get();
			} else {
				diffAcMaximum = Integer.MAX_VALUE;
			}
		} else {
			// Force internal mode
			diffAcMaximum = 0;
		}

		if ((diffBalancing > -1 && diffBalancing < 1) || (diffSurplus > -1 && diffSurplus < 1)
				|| (diffAcMaximum > -1 && diffAcMaximum < 1)) {
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
