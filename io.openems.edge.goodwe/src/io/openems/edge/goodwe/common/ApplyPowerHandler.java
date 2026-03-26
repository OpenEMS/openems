package io.openems.edge.goodwe.common;

import static io.openems.common.utils.IntUtils.maxInt;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.filter.Filter;
import io.openems.edge.common.filter.PT1Filter;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

public final class ApplyPowerHandler {

	private final AbstractGoodWe goodWe;
	private final PT1Filter internalFilter = new PT1Filter(800 /* [ms */);

	public ApplyPowerHandler(AbstractGoodWe goodWe) {
		this.goodWe = goodWe;
	}

	/**
	 * Apply the desired Active-Power Set-Point by setting the appropriate
	 * EMS_POWER_SET and EMS_POWER_MODE settings.
	 *
	 * @param setActivePower        the Active-Power Set-Point
	 * @param controlMode           the {@link ControlMode} to handle the different
	 *                              {@link EmsPowerMode} for the GoodWe battery
	 *                              inverter
	 * @param gridActivePower       the grid active power
	 * @param essActivePower        the ESS active power
	 * @param maxAcImport           the max AC import power
	 * @param maxAcExport           the max AC export power
	 * @param isGlobalFilterEnabled is global {@link Filter} enabled?
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(int setActivePower, ControlMode controlMode, Value<Integer> gridActivePower,
			Value<Integer> essActivePower, Value<Integer> maxAcImport, Value<Integer> maxAcExport,
			boolean isGlobalFilterEnabled) throws OpenemsNamedException {
		// Evaluate MeterCommunicateStatus
		EnumReadChannel meterCommunicateStatusChannel = this.goodWe.channel(GoodWe.ChannelId.METER_COMMUNICATE_STATUS);
		MeterCommunicateStatus meterCommunicateStatus = meterCommunicateStatusChannel.value().asEnum();

		// Calculate PV production and Surplus Power
		var pvProduction = maxInt(0, this.goodWe.calculatePvProduction());
		var surplusPower = maxInt(0, this.goodWe.getSurplusPower());

		// Write-Channels
		final var emsPowerSetChannel = this.goodWe.getEmsPowerSetChannel();
		EnumWriteChannel emsPowerModeChannel = this.goodWe.channel(GoodWe.ChannelId.EMS_POWER_MODE);

		this.apply(setActivePower, controlMode, gridActivePower, essActivePower, maxAcImport, maxAcExport,
				isGlobalFilterEnabled, meterCommunicateStatus, pvProduction, surplusPower, //
				this.goodWe.channel(GoodWe.ChannelId.SMART_MODE_NOT_WORKING_WITH_FILTER)::setNextValue, //
				this.goodWe.channel(GoodWe.ChannelId.NO_SMART_METER_DETECTED)::setNextValue, //
				emsPowerSetChannel::setNextWriteValue, //
				emsPowerModeChannel::setNextWriteValue);
	}

	protected synchronized void apply(int setActivePower, ControlMode controlMode, Value<Integer> gridActivePower,
			Value<Integer> essActivePower, Value<Integer> maxAcImport, Value<Integer> maxAcExport,
			boolean isGlobalFilterEnabled, MeterCommunicateStatus meterCommunicateStatus, int pvProduction,
			int surplusPower, BooleanConsumer setSmartModeNotWorkingWithPidFilter,
			BooleanConsumer setNoSmartMeterDetected, //
			ThrowingConsumer<Long, OpenemsNamedException> writeEmsPowerSet, //
			ThrowingConsumer<EmsPowerMode, OpenemsNamedException> writeEmsPowerMode) throws OpenemsNamedException {

		// Update Warn Channels
		setSmartModeNotWorkingWithPidFilter.accept(//
				checkControlModeWithActiveFilter(controlMode, isGlobalFilterEnabled));

		setNoSmartMeterDetected.accept(//
				checkControlModeRequiresSmartMeter(controlMode, meterCommunicateStatus));

		// Set Channels
		final ApplyPowerHandler.Result apply = calculate(setActivePower, pvProduction, controlMode,
				gridActivePower.get(), essActivePower.get(), maxAcImport.get(), maxAcExport.get(), surplusPower);

		writeEmsPowerMode.accept(apply.emsPowerMode);
		writeEmsPowerSet.accept(this.applyInternalFilter(isGlobalFilterEnabled, essActivePower, maxAcImport.get(),
				maxAcExport.get(), apply));
	}

	/**
	 * If {@link EmsPowerMode} is not {@link EmsPowerMode#AUTO}, apply fallback PID
	 * filter.
	 * 
	 * @param isGlobalFilterEnabled is global {@link Filter} enabled?
	 * @param essActivePower        the Active-Power Set-Point
	 * @param maxAcImport           the max AC import power
	 * @param maxAcExport           the max AC export power
	 * @param apply                 the calculated {@link Result}
	 * @return the filtered EMS-Power-Set value
	 */
	protected long applyInternalFilter(boolean isGlobalFilterEnabled, Value<Integer> essActivePower,
			Integer maxAcImport, Integer maxAcExport, Result apply) {
		return switch (apply.emsPowerMode) {
		case AUTO -> {
			// If Filter is disabled, we still want to update the internal state of the
			// filter to avoid a big jump when enabling it.
			this.internalFilter.reset();
			yield apply.emsPowerSet;
		}

		case BATTERY_STANDBY, BUY_POWER, CHARGE_BAT, CHARGE_PV, CONSERVE, DISCHARGE_BAT, DISCHARGE_PV, EXPORT_AC,
				IMPORT_AC, OFF_GRID, SELL_POWER, STOPPED, UNDEFINED -> {
			if (isGlobalFilterEnabled) {
				yield apply.emsPowerSet;
			}

			if (maxAcImport == null || maxAcExport == null) {
				// Cannot apply filter without limits
				yield apply.emsPowerSet;
			}

			this.internalFilter.setLimits(maxAcImport, maxAcExport);
			var filteredEmsPowerSet = this.internalFilter.applyPT1Filter(apply.emsPowerSet);
			yield filteredEmsPowerSet;
		}
		};
	}

	protected static record Result(EmsPowerMode emsPowerMode, long emsPowerSet) {
	}

	protected static ApplyPowerHandler.Result calculate(int activePowerSetPoint, int pvProduction,
			ControlMode controlMode, Integer gridActivePower, Integer essActivePower, Integer maxAcImport,
			Integer maxAcExport, int surplusPower) throws OpenemsNamedException {
		return switch (controlMode) {
		case INTERNAL //
			-> handleInternalMode();

		case SMART -> {
			if (essActivePower != null) {
				if (gridActivePower != null) {
					// Sufficient data to apply SMART mode
					yield handleSmartMode(activePowerSetPoint, pvProduction, gridActivePower, essActivePower,
							surplusPower);
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
			int essActivePower, int surplusPower) throws OpenemsNamedException {

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
	 * Check current {@link ControlMode} is set to SMART and a {@link Filter} is
	 * enabled. If true warning channel SMART_MODE_NOT_WORKING_WITH_FILTER set to
	 * true, otherwise to false.
	 *
	 * @param controlMode     the {@link ControlMode} to check SMART mode
	 * @param isFilterEnabled if {@link Filter} is enabled
	 * @return SMART_MODE_NOT_WORKING_WITH_FILTER
	 */
	protected static boolean checkControlModeWithActiveFilter(ControlMode controlMode, boolean isFilterEnabled) {
		if (controlMode.equals(ControlMode.SMART) && isFilterEnabled) {
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
