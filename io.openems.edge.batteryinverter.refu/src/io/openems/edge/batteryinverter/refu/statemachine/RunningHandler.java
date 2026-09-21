package io.openems.edge.batteryinverter.refu.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel.S123.S123Ena;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel.S64800.S64800PcsSetOperation;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.type.TypeUtils;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		// REFU requires DC to be present before it can run
		if (!context.battery.isStarted()) {
			return State.UNDEFINED;
		}

		if (inverter.hasFailure()) {
			return State.ERROR;
		}

		if (inverter.isInStandby()) {
			if (context.setActivePower != 0 || context.setReactivePower != 0) {
				inverter.presetZeroPower();
				inverter.setPcsSetOperation(S64800PcsSetOperation.EXIT_STANDBY_MODE);
			}
			inverter._setStartStop(StartStop.START);
			return State.RUNNING;
		}

		if (inverter.getOperatingState() == DefaultSunSpecModel.S103_St.STARTING) {
			inverter._setStartStop(StartStop.START);
			return State.RUNNING;
		}

		if (!inverter.isRunning()) {
			return State.ERROR;
		}

		this.applyPower(context);
		inverter._setStartStop(StartStop.START);
		return State.RUNNING;
	}

	private void applyPower(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();
		var maxApparentPowerOpt = inverter.getMaxApparentPower().asOptional();
		var maxApparentPower = maxApparentPowerOpt.map(Number::doubleValue).orElse(0.0);

		if (maxApparentPower <= 0) {
			inverter.presetZeroPower();
			return;
		}

		float wSetPct = TypeUtils.fitWithin(-100F, 100F, //
				(float) (context.setActivePower * 100.0 / maxApparentPower));
		float varSetPct = TypeUtils.fitWithin(-100F, 100F,
				(float) (context.setReactivePower * 100.0 / maxApparentPower));

		FloatWriteChannel wMaxLimPctChannel = inverter.getSunSpecChannelOrError(RefuSunSpecModel.S123.W_MAX_LIM_PCT);
		wMaxLimPctChannel.setNextWriteValue(wSetPct);

		EnumWriteChannel wMaxLimEnaChannel = inverter.getSunSpecChannelOrError(RefuSunSpecModel.S123.W_MAX_LIM_ENA);
		wMaxLimEnaChannel.setNextWriteValue(S123Ena.ENABLED);

		FloatWriteChannel varWMaxPctChannel = inverter.getSunSpecChannelOrError(RefuSunSpecModel.S123.VAR_W_MAX_PCT);
		varWMaxPctChannel.setNextWriteValue(varSetPct);

		EnumWriteChannel varPctEnaChannel = inverter.getSunSpecChannelOrError(RefuSunSpecModel.S123.VAR_PCT_ENA);
		varPctEnaChannel.setNextWriteValue(S123Ena.ENABLED);
	}
}