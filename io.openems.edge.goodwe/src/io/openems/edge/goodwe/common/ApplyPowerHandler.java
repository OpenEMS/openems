package io.openems.edge.goodwe.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

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
	 * @param isPidEnabled    if PID Filter is enabled
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(AbstractGoodWe goodWe, int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower, Value<Integer> essActivePower, Value<Integer> maxAcImport,
			Value<Integer> maxAcExport, boolean isPidEnabled) throws OpenemsNamedException {

		// Update Warn Channels
		this.checkControlModeWithActivePid(goodWe, controlMode, isPidEnabled);
		this.checkControlModeRequiresSmartMeter(goodWe, controlMode);

		// calculate pv production
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

	private static record Result(EmsPowerMode emsPowerMode, int emsPowerSet) {
	}

	private static ApplyPowerHandler.Result calculate(AbstractGoodWe goodWe, int activePowerSetPoint, int pvProduction,
			ControlMode controlMode, int gridActivePower, int essActivePower, int maxAcImport, int maxAcExport)
			throws OpenemsNamedException {
		return switch (controlMode) {
		case INTERNAL //
			-> handleInternalMode();
		case SMART //
			-> handleSmartMode(goodWe, activePowerSetPoint, pvProduction, gridActivePower, essActivePower, maxAcImport,
					maxAcExport);
		case REMOTE //
			-> handleRemoteMode(activePowerSetPoint, pvProduction);
		};
	}

	private static Result handleInternalMode() {
		return new Result(EmsPowerMode.AUTO, 0);
	}

	private static Result handleSmartMode(AbstractGoodWe goodWe, int activePowerSetPoint, int pvProduction,
			int gridActivePower, int essActivePower, int maxAcImport, int maxAcExport) throws OpenemsNamedException {

		// Is Surplus-Feed-In active?
		final var surplusPower = goodWe.getSurplusPower();
		var diffSurplus = Integer.MAX_VALUE;
		if (surplusPower != null && surplusPower > 0 && activePowerSetPoint != 0) {
			diffSurplus = activePowerSetPoint - surplusPower;
		}

		// Is Balancing to zero active?
		var diffBalancing = activePowerSetPoint - (gridActivePower + essActivePower);

		if (diffBalancing > -1 && diffBalancing < 1 || diffSurplus > -1 && diffSurplus < 1) {

			// avoid rounding errors
			return handleInternalMode();
		}

		return handleRemoteMode(activePowerSetPoint, pvProduction);
	}

	private static Result handleRemoteMode(int activePowerSetPoint, int pvProduction) {
		// TODO PV curtail: (surplus power == setpoint && battery soc == 100% => PV
		// curtail)
		if (activePowerSetPoint < 0) {
			return new Result(EmsPowerMode.CHARGE_BAT, activePowerSetPoint * -1 + pvProduction);
		}
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
	}

	/**
	 * Check current {@link ControlMode} is set to SMART and PID filter is enabled.
	 * If true warning channel SMART_MODE_NOT_WORKING_WITH_PID_FILTER set to true,
	 * otherwise to false.
	 *
	 * @param goodWe       the GoodWe - either Battery-Inverter or ESS
	 * @param controlMode  the {@link ControlMode} to check SMART mode
	 * @param isPidEnabled if PID filter is enabled
	 */
	private void checkControlModeWithActivePid(AbstractGoodWe goodWe, ControlMode controlMode, boolean isPidEnabled) {
		var enableWarning = false;
		if (controlMode.equals(ControlMode.SMART) && isPidEnabled) {
			enableWarning = true;
		}

		goodWe.channel(GoodWe.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER).setNextValue(enableWarning);
	}

	/**
	 * Check if configured {@link ControlMode} is possible - depending on if a
	 * GoodWe Smart Meter is connected or not.
	 *
	 * @param goodWe      the GoodWe - either Battery-Inverter or ESS
	 * @param controlMode the {@link ControlMode} to check SMART mode
	 */
	private void checkControlModeRequiresSmartMeter(AbstractGoodWe goodWe, ControlMode controlMode) {
		EnumReadChannel meterCommunicateStatusChannel = goodWe.channel(GoodWe.ChannelId.METER_COMMUNICATE_STATUS);
		MeterCommunicateStatus meterCommunicateStatus = meterCommunicateStatusChannel.value().asEnum();

		var enableWarning = switch (meterCommunicateStatus) {
		case UNDEFINED -> //
			// We don't know if GoodWe Smart Meter is connected. Either not read yet (on
			// startup) or DSP version too low.
			false;

		case OK ->
			// GoodWe Smart Meter is connected.
			false;

		case NG //
			-> switch (controlMode) {
			case REMOTE ->
				// REMOTE mode is ok without GoodWe Smart Meter
				false;
			case INTERNAL, SMART ->
				// INTERNAL and SMART mode require a GoodWe Smart Meter
				true;
			};
		};

		goodWe.channel(GoodWe.ChannelId.NO_SMART_METER_DETECTED).setNextValue(enableWarning);
	}

}
