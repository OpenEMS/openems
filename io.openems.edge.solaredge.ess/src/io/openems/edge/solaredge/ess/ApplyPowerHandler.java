package io.openems.edge.solaredge.ess;

import static java.lang.Math.round;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.solaredge.ess.enums.AcChargePolicy;
import io.openems.edge.solaredge.ess.enums.CommandMode;
import io.openems.edge.solaredge.ess.enums.ControlMode;
import io.openems.edge.solaredge.ess.enums.MeterCommunicateStatus;
import io.openems.edge.solaredge.ess.enums.SEControlMode;

public class ApplyPowerHandler {

	static final float DISCHARGE_EFFICIENCY_FACTOR = 0.95F;
	
	/**
	 * Apply the desired Active-Power Set-Point by setting the appropriate
	 * REMOTE_CONTROL_COMMAND_MODE and CHARGE/DISCHARGE_LIMIT settings.
	 *
	 * @param solarEdge       the SolarEdge ESS
	 * @param setActivePower  the Active-Power Set-Point
	 * @param controlMode     the {@link ControlMode} to handle the different
	 *                        {@link CommandMode} for the solarEdge battery inverter
	 * @param gridActivePower the grid active power
	 * @param essActivePower  the ESS active power
	 * @param isPidEnabled    if PID Filter is enabled
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(SolarEdgeEss solarEdge, int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower, Value<Integer> essActivePower, boolean isPidEnabled) throws OpenemsNamedException {

		// Update Warn Channels
		this.checkControlModeRequiresRemoteControl(solarEdge, controlMode);
		this.checkControlModeRequiresACCharge(solarEdge, controlMode);
		this.checkControlModeWithActivePid(solarEdge, controlMode, isPidEnabled);
		this.checkControlModeRequiresSmartMeter(solarEdge, controlMode);

		// get pv production
		int pvProduction = TypeUtils.max(0, solarEdge.getPvProduction());

		final ApplyPowerHandler.Result apply;
		if (gridActivePower.isDefined() && essActivePower.isDefined()) {
			apply = calculate(solarEdge, setActivePower, pvProduction, controlMode, gridActivePower.get(), essActivePower.get());
		} else {
			// If any Channel Value is not available: fall back to AUTO mode
			apply = handleInternalMode(solarEdge);
		}
				
		// Set Channels
		IntegerWriteChannel remoteControlCommandTimeoutChannel = solarEdge.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_TIMEOUT);
		remoteControlCommandTimeoutChannel.setNextWriteValue(60);
		IntegerWriteChannel remoteControlCommandChargeLimitChannel = solarEdge.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_CHARGE_LIMIT);
		remoteControlCommandChargeLimitChannel.setNextWriteValue(apply.chargeLimit);
		IntegerWriteChannel remoteControlCommandDischargeLimitChannel = solarEdge.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT);
		remoteControlCommandDischargeLimitChannel.setNextWriteValue(apply.dischargeLimit);		
		EnumWriteChannel remoteControlCommandModeChannel = solarEdge.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE);
		remoteControlCommandModeChannel.setNextWriteValue(apply.commandMode);
	}

	private static record Result(CommandMode commandMode, int chargeLimit, int dischargeLimit) {
	}

	private static ApplyPowerHandler.Result calculate(SolarEdgeEss solarEdge, int activePowerSetPoint, int pvProduction,
			ControlMode controlMode, int gridActivePower, int essActivePower)
			throws OpenemsNamedException {
		return switch (controlMode) {
		case INTERNAL //
			-> handleInternalMode(solarEdge);
		case SMART //
			-> handleSmartMode(solarEdge, activePowerSetPoint, pvProduction, gridActivePower, essActivePower);
		case REMOTE //
			-> handleRemoteMode(solarEdge, activePowerSetPoint, pvProduction);
		};
	}

	private static Result handleInternalMode(SolarEdgeEss solarEdge) {
		return new Result(CommandMode.AUTO, solarEdge.getBattery1MaxChargeContinuesPower().orElse(0), solarEdge.getBattery1MaxDischargeContinuesPower().orElse(0));
	}

	private static Result handleSmartMode(SolarEdgeEss solarEdge, int activePowerSetPoint, int pvProduction,
			int gridActivePower, int essActivePower) throws OpenemsNamedException {

		// Is Surplus-Feed-In active?
		final var surplusPower = solarEdge.getSurplusPower();
		var diffSurplus = Integer.MAX_VALUE;
		if (surplusPower != null && surplusPower > 0 && activePowerSetPoint != 0) {
			diffSurplus = activePowerSetPoint - surplusPower;
		}

		// Is Balancing to zero active?
		var diffBalancing = activePowerSetPoint - (gridActivePower + essActivePower);

		if ((diffBalancing > -1 && diffBalancing < 1 || diffSurplus > -1 && diffSurplus < 1) && activePowerSetPoint != 0) {
			// avoid rounding errors
			return handleInternalMode(solarEdge);
		}	

		return handleRemoteMode(solarEdge, activePowerSetPoint, pvProduction);
	}

	private static Result handleRemoteMode(SolarEdgeEss solarEdge, int activePowerSetPoint, int pvProduction) {
		
		// TODO PV curtail: (surplus power == setpoint && battery soc == 100% => PV
		// curtail)
		if (activePowerSetPoint < 0) {
			var result = activePowerSetPoint * -1 + pvProduction;
			if(solarEdge.getSoc().orElse(100)>=100) {
				// battery full, limit charge power to zero
				result = 0;
			}
			else if(result>solarEdge.getBattery1MaxChargeContinuesPower().orElse(0)) {
				// limit to max charge power
				result = solarEdge.getBattery1MaxChargeContinuesPower().orElse(0);
			}
			return new Result(CommandMode.CHARGE_BAT, result, 0);
		}
		if (pvProduction >= activePowerSetPoint) {
			// Set-Point is positive && less than PV-Production -> feed PV partly to grid +
			// charge battery
			// On Surplus Feed-In PV == Set-Point => CHARGE_BAT 0
			var result = round((pvProduction - activePowerSetPoint)*DISCHARGE_EFFICIENCY_FACTOR);
			if(solarEdge.getSoc().orElse(100)>=100) {
				// battery full, limit charge power to zero -> required for Set-Point 0
				result = 0;
			}
			else if(result>solarEdge.getBattery1MaxChargeContinuesPower().orElse(0)) {
				// limit to max charge power
				result = solarEdge.getBattery1MaxChargeContinuesPower().orElse(0);
			}
			return new Result(CommandMode.CHARGE_BAT, result, 0);
		} else {
			// Set-Point is positive && bigger than PV-Production -> feed all PV to grid +
			// discharge battery
			var result = round((activePowerSetPoint - pvProduction)/DISCHARGE_EFFICIENCY_FACTOR);			
			//log.info("[3] activePowerSetPoint: "+activePowerSetPoint+ ", pvProducution: "+pvProduction+", allowed "+solarEdge.getBattery1MaxDischargeContinuesPower().orElse(0)+", DISCHARGE_BAT "+result+" ("+(activePowerSetPoint-pvProduction)+")");
			if(solarEdge.getSoc().orElse(0)<=10) {
				// battery empty (=SOC equals or less than soc_min of 10), limit charge power to zero -> required for Set-Point 0
				result = 0;
			}
			else if(result>solarEdge.getBattery1MaxDischargeContinuesPower().orElse(0)) {
				// limit to max discharge power
				result = solarEdge.getBattery1MaxDischargeContinuesPower().orElse(0);
			}
			return new Result(CommandMode.DISCHARGE_BAT, 0, result);
		}
	}
	
	/**
	 * Check if {@link SEControlMode} is set to Remote Control.
	 * If false warning channel REMOTE_CONTROL_NOT_ENABLED is set to true,
	 * otherwise to false.
	 *
	 * @param solarEdge   the SolarEdge ESS
	 * @param controlMode  the {@link ControlMode} to check control mode
	 */
	private void checkControlModeRequiresRemoteControl(SolarEdgeEss solarEdge, ControlMode controlMode) {
		EnumReadChannel seControlModeChannel = solarEdge.channel(SolarEdgeEss.ChannelId.STORAGE_CONTROL_MODE);
		SEControlMode seControlMode = seControlModeChannel.value().asEnum();
		
		var enableWarning = switch (seControlMode) {
		case UNDEFINED -> //
			// We don't know the Storage Control Mode. Not ready yet (on startup)
			false;

		case REMOTE_CONTROL ->
			// Storage Control Mode is set to Remote Control.
			false;

		case DISABLED, MAX_SELF_CONSUMPTION, TIME_OF_USE, BACKUP_ONLY //
			-> switch (controlMode) {
			case INTERNAL ->
				// INTERNAL mode is ok without Remote Control Mode
				false;
			case REMOTE, SMART ->
				// REMOTE and SMART mode requires Remote Control Mode
				true;
			};
		};

		solarEdge.channel(SolarEdgeEss.ChannelId.REMOTE_CONTROL_NOT_ENABLED).setNextValue(enableWarning);
	}	

	/**
	 * Check if {@link AcChargePolicy} is set to Always allowed.
	 * If false warning channel AC_CHARGE_NOT_ENABLED is set to true,
	 * otherwise to false.
	 *
	 * @param solarEdge   the SolarEdge ESS
	 * @param controlMode  the {@link ControlMode} to check control mode
	 */
	private void checkControlModeRequiresACCharge(SolarEdgeEss solarEdge, ControlMode controlMode) {
		EnumReadChannel acChargePolicyChannel = solarEdge.channel(SolarEdgeEss.ChannelId.STORAGE_AC_CHARGE_POLICY);
		AcChargePolicy acChargePolicy = acChargePolicyChannel.value().asEnum();
		
		var enableWarning = switch (acChargePolicy) {
		case UNDEFINED -> //
			// We don't know the AC Charge Policy. Not ready yet (on startup)
			false;

		case ALWAYS_ALLOWED ->
			// AC Charge Policy is set to Always allowed.
			false;

		case DISABLED, FIXED_ENERGY_LIMIT, PERCENT_OF_PRODUCTION //
			-> switch (controlMode) {
			case INTERNAL ->
				// INTERNAL mode is ok with any AC Charge Policy
				false;
			case REMOTE, SMART ->
				// REMOTE and SMART mode requires AC Charge Policy set to Always allowed
				true;
			};
		};		
		
		solarEdge.channel(SolarEdgeEss.ChannelId.AC_CHARGE_NOT_ENABLED).setNextValue(enableWarning);
	}	

	/**
	 * Check current {@link ControlMode} is set to SMART and PID filter is enabled.
	 * If true warning channel SMART_MODE_NOT_WORKING_WITH_PID_FILTER set to true,
	 * otherwise to false.
	 *
	 * @param solarEdge    the SolarEdge ESS
	 * @param controlMode  the {@link ControlMode} to check SMART mode
	 * @param isPidEnabled if PID filter is enabled
	 */
	private void checkControlModeWithActivePid(SolarEdgeEss solarEdge, ControlMode controlMode, boolean isPidEnabled) {
		var enableWarning = false;
		if (controlMode.equals(ControlMode.SMART) && isPidEnabled) {
			enableWarning = true;
		}

		solarEdge.channel(SolarEdgeEss.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER).setNextValue(enableWarning);
	}

	/**
	 * Check if configured {@link ControlMode} is possible - depending on if a
	 * solarEdge Smart Meter is connected or not.
	 *
	 * @param solarEdge   the SolarEdge ESS
	 * @param controlMode the {@link ControlMode} to check control mode
	 */
	private void checkControlModeRequiresSmartMeter(SolarEdgeEss solarEdge, ControlMode controlMode) {
		EnumReadChannel meterCommunicateStatusChannel = solarEdge.channel(SolarEdgeEss.ChannelId.METER_COMMUNICATE_STATUS);
		MeterCommunicateStatus meterCommunicateStatus = meterCommunicateStatusChannel.value().asEnum();

		var enableWarning = switch (meterCommunicateStatus) {
		case UNDEFINED -> //
			// We don't know if SolarEdge Smart Meter is connected. Not ready yet (on startup)
			false;

		case OK ->
			// SolarEdge Smart Meter is connected.
			false;

		case NO_METER //
			-> switch (controlMode) {
			case REMOTE ->
				// REMOTE mode is ok without SolarEdge Smart Meter
				false;
			case INTERNAL, SMART ->
				// INTERNAL and SMART mode require a SolarEdge Smart Meter
				true;
			};
		};

		solarEdge.channel(SolarEdgeEss.ChannelId.NO_SMART_METER_DETECTED).setNextValue(enableWarning);
	}	

}