package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.BatteryBalanceStatus;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class CheckBalancingHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(CheckBalancingHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		context.refreshMeasurementChargingMinVoltageFromEss();

		final Integer voltageDelta = context.calculateCellVoltageDelta();
		context.setVoltageDelta(voltageDelta);

		final int maxAllowedDelta = EssSohCycleConstants.MAX_CELL_VOLTAGE_DIFFERENCE_MV;
		final int soc = context.ess.getSoc().orElse(0);

		final BatteryBalanceStatus balanceStatus;
		if (voltageDelta == null) {
			balanceStatus = BatteryBalanceStatus.NOT_MEASURED;
			context.logInfo(log, String.format(
					"%s: SoC=%d%%, cell voltage data not available",
					StateMachine.State.CHECK_BALANCING.getName(), soc));
		} else {
			final boolean isBalanced = context.isBatteryBalanced(maxAllowedDelta);
			balanceStatus = isBalanced ? BatteryBalanceStatus.BALANCED : BatteryBalanceStatus.NOT_BALANCED;
			context.logInfo(log, String.format(
					"%s: SoC=%d%%, voltage delta=%d mV, threshold=%d mV, balanced=%s",
					StateMachine.State.CHECK_BALANCING.getName(), soc, voltageDelta,
					maxAllowedDelta, isBalanced ? "yes" : "no"));
		}
		ChannelUtils.setValue(context.getParent(), ControllerEssSohCycle.ChannelId.IS_BATTERY_BALANCED, balanceStatus);

		// do not abort the cycle if not balanced - just log and continue
		return StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING;
	}
}
