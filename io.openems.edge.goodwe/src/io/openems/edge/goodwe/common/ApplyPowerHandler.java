package io.openems.edge.goodwe.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

public final class ApplyPowerHandler {

	private ApplyPowerHandler() {
	}

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
	public static synchronized void apply(AbstractGoodWe goodWe, int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower, Value<Integer> essActivePower, Value<Integer> maxAcImport,
			Value<Integer> maxAcExport, boolean isPidEnabled) throws OpenemsNamedException {
		// Evaluate MeterCommunicateStatus
		EnumReadChannel meterCommunicateStatusChannel = goodWe.channel(GoodWe.ChannelId.METER_COMMUNICATE_STATUS);
		MeterCommunicateStatus meterCommunicateStatus = meterCommunicateStatusChannel.value().asEnum();

		// Calculate PV production and Surplus Power
		var pvProduction = TypeUtils.max(0, goodWe.calculatePvProduction());
		var surplusPower = TypeUtils.max(0, goodWe.getSurplusPower());

		// Write-Channels
		IntegerWriteChannel emsPowerSetChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_SET);
		EnumWriteChannel emsPowerModeChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_MODE);

		apply(setActivePower, controlMode, gridActivePower, essActivePower, maxAcImport, maxAcExport, isPidEnabled,
				meterCommunicateStatus, pvProduction, surplusPower, //
				goodWe.channel(GoodWe.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER)::setNextValue, //
				goodWe.channel(GoodWe.ChannelId.NO_SMART_METER_DETECTED)::setNextValue, //
				emsPowerSetChannel::setNextWriteValue, //
				emsPowerModeChannel::setNextWriteValue);
	}

	protected static synchronized void apply(int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower, Value<Integer> essActivePower, Value<Integer> maxAcImport,
			Value<Integer> maxAcExport, boolean isPidEnabled, MeterCommunicateStatus meterCommunicateStatus,
			int pvProduction, int surplusPower, //
			BooleanConsumer setSmartModeNotWorkingWithPidFilter, BooleanConsumer setNoSmartMeterDetected, //
			ThrowingConsumer<Integer, OpenemsNamedException> writeEmsPowerSet, //
			ThrowingConsumer<EmsPowerMode, OpenemsNamedException> writeEmsPowerMode) throws OpenemsNamedException {

		// Update Warn Channels
		setSmartModeNotWorkingWithPidFilter.accept(//
				checkControlModeWithActivePid(controlMode, isPidEnabled));

		setNoSmartMeterDetected.accept(//
				checkControlModeRequiresSmartMeter(controlMode, meterCommunicateStatus));

		// Set Channels
		final ApplyPowerHandler.Result apply = calculate(setActivePower, pvProduction, controlMode,
				gridActivePower.get(), essActivePower.get(), maxAcImport.get(), maxAcExport.get(), surplusPower);
		writeEmsPowerSet.accept(apply.emsPowerSet);
		writeEmsPowerMode.accept(apply.emsPowerMode);
	}

	protected static record Result(EmsPowerMode emsPowerMode, int emsPowerSet) {
	}

	protected static ApplyPowerHandler.Result calculate(int activePowerSetPoint, int pvProduction,
			ControlMode controlMode, Integer gridActivePower, Integer essActivePower, Integer maxAcImport,
			Integer maxAcExport, int surplusPower) throws OpenemsNamedException {
		return switch (controlMode) {
		case INTERNAL //
			-> handleInternalMode();

		case SMART -> {
			if (essActivePower != null && maxAcImport != null && maxAcImport != null) {
				if (gridActivePower != null) {
					// Sufficient data to apply SMART mode
					yield handleSmartMode(activePowerSetPoint, pvProduction, gridActivePower, essActivePower,
							maxAcImport, maxAcExport, surplusPower);
				} else {
					// Only Grid-Meter is not available -> SMART is not possible, but can still
					// apply REMOTE
					yield handleRemoteMode(activePowerSetPoint, pvProduction);
				}
			}
			// If any Channel Value is not available: fall back to AUTO mode
			yield handleInternalMode();
		}

		case REMOTE //
			-> handleRemoteMode(activePowerSetPoint, pvProduction);
		};
	}

	private static Result handleInternalMode() {
		return new Result(EmsPowerMode.AUTO, 0);
	}

	private static Result handleSmartMode(int activePowerSetPoint, int pvProduction, int gridActivePower,
			int essActivePower, int maxAcImport, int maxAcExport, int surplusPower) throws OpenemsNamedException {

		// Is Surplus-Feed-In active?
		var diffSurplus = Integer.MAX_VALUE;
		if (surplusPower > 0 && activePowerSetPoint != 0) {
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
	 * @param controlMode  the {@link ControlMode} to check SMART mode
	 * @param isPidEnabled if PID filter is enabled
	 * @return SMART_MODE_NOT_WORKING_WITH_PID_FILTER
	 */
	protected static boolean checkControlModeWithActivePid(ControlMode controlMode, boolean isPidEnabled) {
		if (controlMode.equals(ControlMode.SMART) && isPidEnabled) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if configured {@link ControlMode} is possible - depending on if a
	 * GoodWe Smart Meter is connected or not.
	 *
	 * @param controlMode            the {@link ControlMode} to check SMART mode
	 * @param meterCommunicateStatus the {@link MeterCommunicateStatus}
	 * @return NO_SMART_METER_DETECTED
	 */
	protected static boolean checkControlModeRequiresSmartMeter(ControlMode controlMode,
			MeterCommunicateStatus meterCommunicateStatus) {
		return switch (meterCommunicateStatus) {
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
	}

}
