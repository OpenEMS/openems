package io.openems.edge.batteryinverter.refu88k.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu88k.RefuStore88kChannelId;
import io.openems.edge.batteryinverter.refu88k.enums.VArPctEna;
import io.openems.edge.batteryinverter.refu88k.enums.WMaxLimEna;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	private LocalDateTime timeNoPower;

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		switch (context.component.getOperatingState()) {

		case STARTED:
		case THROTTLED:
		case MPPT:
			// Mark as started
			context.component._setStartStop(StartStop.START);
			// Apply Active and Reactive Power Set-Points
			this.applyPower(context);
			return State.RUNNING;
		case FAULT:
			return State.ERROR;
		case OFF:
		case SLEEPING:
		case STARTING:
		case SHUTTING_DOWN:
		case STANDBY:
		case UNDEFINED:
			return State.UNDEFINED;
		}

		return State.UNDEFINED;
	}

	/**
	 * Applies the Active and Reactive Power Set-Points.
	 * 
	 * @param context the {@link Context}
	 * @throws OpenemsNamedException on error
	 */

	private void applyPower(Context context) throws OpenemsNamedException {
		doGridConnectedHandling(context, context.setActivePower, context.setReactivePower);

		IntegerReadChannel maxApparentPowerChannel = context.component
				.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
//		IntegerReadChannel maxApparentPowerChannel = context.component
//				.channel(RefuStore88kChannelId.W_RTG);
		int maxApparentPower = maxApparentPowerChannel.value().getOrError();

		int wSetPct = 0;
		int varSetPct = 0;

		// Calculate Active Power as a percentage of WMAX
		wSetPct = ((1000 * context.setActivePower) / maxApparentPower);
		// Calculate Reactive Power as a percentage of WMAX
		varSetPct = ((100 * context.setReactivePower) / maxApparentPower);

		IntegerWriteChannel wMaxLimPctChannel = context.component.channel(RefuStore88kChannelId.W_MAX_LIM_PCT);
		EnumWriteChannel wMaxLim_EnaChannel = context.component.channel(RefuStore88kChannelId.W_MAX_LIM_ENA);

		IntegerWriteChannel varMaxLimPctChannel = context.component.channel(RefuStore88kChannelId.VAR_W_MAX_PCT);
		EnumWriteChannel varMaxLim_EnaChannel = context.component.channel(RefuStore88kChannelId.VAR_PCT_ENA);

		wMaxLimPctChannel.setNextWriteValue(wSetPct);
		wMaxLim_EnaChannel.setNextWriteValue(WMaxLimEna.ENABLED);

		varMaxLimPctChannel.setNextWriteValue(varSetPct);
		varMaxLim_EnaChannel.setNextWriteValue(VArPctEna.ENABLED);
	}

//	private boolean checkIfPowerIsAllowed(Context context) {
//
//		// If the battery system is not ready no power can be applied!
//		if (context.battery.getStartStop() != StartStop.START) {
//			return false;
//		}
//
//		// Read important Channels from battery
//		int optV = context.battery.getVoltage().orElse(0);
//		int disMaxA = context.battery.getDischargeMaxCurrent().orElse(0);
//		int chaMaxA = context.battery.getChargeMaxCurrent().orElse(0);
//
//		// Calculate absolute Value allowedCharge and allowed Discharge from battery
//		double absAllowedCharge = Math.abs((chaMaxA * optV) / (EFFICIENCY_FACTOR));
//		double absAllowedDischarge = Math.abs((disMaxA * optV) * (EFFICIENCY_FACTOR));
//
//		// Determine allowedCharge and allowedDischarge from Inverter
//		if (absAllowedCharge > MAX_APPARENT_POWER) {
//			this.getAllowedCharge().setNextValue(MAX_APPARENT_POWER * -1);
//		} else {
//			this.getAllowedCharge().setNextValue(absAllowedCharge * -1);
//		}
//
//		if (absAllowedDischarge > MAX_APPARENT_POWER) {
//			this.getAllowedDischarge().setNextValue(MAX_APPARENT_POWER);
//		} else {
//			this.getAllowedDischarge().setNextValue(absAllowedDischarge);
//		}
//	}

	/**
	 * 
	 * Checks if power is required from the system!
	 * 
	 * @throws OpenemsNamedException
	 * 
	 */

	private void doGridConnectedHandling(Context context, int activePower, int reactivePower)
			throws OpenemsNamedException {
		if (activePower == 0 && reactivePower == 0) {
			if (timeNoPower == null) {
				timeNoPower = LocalDateTime.now();
			}
			if ((timeNoPower.plusSeconds(context.config.timeLimitNoPower())).isBefore(LocalDateTime.now())) {
				context.component.enterStartedMode();
			}
		} else {
			timeNoPower = null;
			context.component.enterThrottledMpptMode();
		}
	}

}
