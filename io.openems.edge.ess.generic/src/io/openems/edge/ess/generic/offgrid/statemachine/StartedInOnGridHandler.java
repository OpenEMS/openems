package io.openems.edge.ess.generic.offgrid.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StartedInOnGridHandler extends StateHandler<OffGridState, Context> {

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;
		final var inverter = context.batteryInverter;

		if (ess.hasFaults()) {
			return OffGridState.UNDEFINED;
		}

		if (!battery.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		if (!inverter.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		setValue(ess, SymmetricEss.ChannelId.GRID_MODE, GridMode.ON_GRID);
		ess._setStartStop(StartStop.START);
		return OffGridState.STARTED_IN_ON_GRID;

	}
}
