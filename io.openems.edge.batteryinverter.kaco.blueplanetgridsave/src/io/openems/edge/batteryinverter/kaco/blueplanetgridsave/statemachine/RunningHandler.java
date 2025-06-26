package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.type.TypeUtils;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();
		if (inverter.hasFailure() || !inverter.isRunning()) {
			return State.ERROR;
		}

		this.applyPower(context);
		inverter._setStartStop(StartStop.START);
		return State.RUNNING;
	}

	/**
	 * Applies the Active and Reactive Power Set-Points.
	 *
	 * @param context the {@link Context}
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		IntegerReadChannel maxApparentPowerChannel = inverter
				.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
		int maxApparentPower = maxApparentPowerChannel.value().getOrError();

		// Active Power Set-Point is set in % of maximum active power
		FloatWriteChannel wSetPctChannel = inverter.getSunSpecChannelOrError(KacoSunSpecModel.S64201.W_SET_PCT);
		var wSetPct = context.setActivePower * 100F / maxApparentPower;
		wSetPct = TypeUtils.fitWithin(-100F, 100F, wSetPct);
		wSetPctChannel.setNextWriteValue(wSetPct);

		// Reactive Power Set-Point is set in % of maximum active power
		FloatWriteChannel varSetPctChannel = inverter.getSunSpecChannelOrError(KacoSunSpecModel.S64201.VAR_SET_PCT);
		var varSetPct = context.setReactivePower * 100F / maxApparentPower;
		varSetPctChannel.setNextWriteValue(varSetPct);
	}
}
