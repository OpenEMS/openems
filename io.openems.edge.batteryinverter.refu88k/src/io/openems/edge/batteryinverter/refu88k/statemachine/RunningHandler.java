package io.openems.edge.batteryinverter.refu88k.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu88k.RefuStore88k;
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
		RefuStore88k inverter = context.getParent();

		if (inverter.hasFaults()) {
			return State.UNDEFINED;
		}

		switch (inverter.getOperatingState()) {

		case STARTED:
		case THROTTLED:
		case MPPT:
			// Mark as started
			inverter._setStartStop(StartStop.START);
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
		RefuStore88k inverter = context.getParent();
		doGridConnectedHandling(context, context.setActivePower, context.setReactivePower);

		IntegerReadChannel maxApparentPowerChannel = inverter
				.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
		int maxApparentPower = maxApparentPowerChannel.value().getOrError();

		int wSetPct = 0;
		int varSetPct = 0;

		// Calculate Active Power as a percentage of WMAX
		wSetPct = ((1000 * context.setActivePower) / maxApparentPower);
		// Calculate Reactive Power as a percentage of WMAX
		varSetPct = ((100 * context.setReactivePower) / maxApparentPower);

		IntegerWriteChannel wMaxLimPctChannel = inverter.channel(RefuStore88kChannelId.W_MAX_LIM_PCT);
		EnumWriteChannel wMaxLim_EnaChannel = inverter.channel(RefuStore88kChannelId.W_MAX_LIM_ENA);

		IntegerWriteChannel varMaxLimPctChannel = inverter.channel(RefuStore88kChannelId.VAR_W_MAX_PCT);
		EnumWriteChannel varMaxLim_EnaChannel = inverter.channel(RefuStore88kChannelId.VAR_PCT_ENA);

		wMaxLimPctChannel.setNextWriteValue(wSetPct);
		wMaxLim_EnaChannel.setNextWriteValue(WMaxLimEna.ENABLED);

		varMaxLimPctChannel.setNextWriteValue(varSetPct);
		varMaxLim_EnaChannel.setNextWriteValue(VArPctEna.ENABLED);
	}

	/**
	 * 
	 * Checks if power is required from the system!
	 * 
	 * @throws OpenemsNamedException
	 * 
	 */

	private void doGridConnectedHandling(Context context, int activePower, int reactivePower)
			throws OpenemsNamedException {
		RefuStore88k inverter = context.getParent();

		if (activePower == 0 && reactivePower == 0) {
			if (timeNoPower == null) {
				timeNoPower = LocalDateTime.now();
			}
			if ((timeNoPower.plusSeconds(context.config.timeLimitNoPower())).isBefore(LocalDateTime.now())) {
				inverter.enterStartedMode();
			}
		} else {
			timeNoPower = null;
			inverter.enterThrottledMpptMode();
		}
	}

}
