package io.openems.edge.batteryinverter.refu88k.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu88k.BatteryInverterRefuStore88k;
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
		var inverter = context.getParent();

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
		var inverter = context.getParent();
		this.doGridConnectedHandling(context, context.setActivePower, context.setReactivePower);

		IntegerReadChannel maxApparentPowerChannel = inverter
				.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
		int maxApparentPower = maxApparentPowerChannel.value().getOrError();

		var wSetPct = 0;
		var varSetPct = 0;

		// Calculate Active Power as a percentage of WMAX
		wSetPct = 1000 * context.setActivePower / maxApparentPower;
		// Calculate Reactive Power as a percentage of WMAX
		varSetPct = 100 * context.setReactivePower / maxApparentPower;

		IntegerWriteChannel wMaxLimPctChannel = inverter.channel(BatteryInverterRefuStore88k.ChannelId.W_MAX_LIM_PCT);
		wMaxLimPctChannel.setNextWriteValue(wSetPct);

		EnumWriteChannel wMaxLimEnaChannel = inverter.channel(BatteryInverterRefuStore88k.ChannelId.W_MAX_LIM_ENA);
		wMaxLimEnaChannel.setNextWriteValue(WMaxLimEna.ENABLED);

		IntegerWriteChannel varMaxLimPctChannel = inverter.channel(BatteryInverterRefuStore88k.ChannelId.VAR_W_MAX_PCT);
		varMaxLimPctChannel.setNextWriteValue(varSetPct);

		EnumWriteChannel varMaxLimEnaChannel = inverter.channel(BatteryInverterRefuStore88k.ChannelId.VAR_PCT_ENA);
		varMaxLimEnaChannel.setNextWriteValue(VArPctEna.ENABLED);
	}

	/**
	 * Checks if power is required from the system.
	 * 
	 * @param context       the {@link Context}
	 * @param activePower   the active power setpoint
	 * @param reactivePower the reactive power setpoint
	 */
	private void doGridConnectedHandling(Context context, int activePower, int reactivePower)
			throws OpenemsNamedException {
		var inverter = context.getParent();

		if (activePower == 0 && reactivePower == 0) {
			if (this.timeNoPower == null) {
				this.timeNoPower = LocalDateTime.now();
			}
			if (this.timeNoPower.plusSeconds(context.config.timeLimitNoPower()).isBefore(LocalDateTime.now())) {
				inverter.enterStartedMode();
			}
		} else {
			this.timeNoPower = null;
			inverter.enterThrottledMpptMode();
		}
	}

}
