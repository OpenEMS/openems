package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.ChannelId.INVERTER_RESTART_STARTING;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.ChannelId.INVERTER_RESTART_STOPPING;
import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.timedata.Timeout;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

/**
 * Handler for the RESTART state.
 *
 * <p>
 * The RESTART state is responsible for restarting the inverter by first
 * stopping it and then starting it again. During this process, the inverter
 * remains logically in the started state within the ESS context to avoid a
 * complete shutdown of the ESS system.
 *
 * A restart is required for certain external conditions, for example after an
 * NA protection signal, in order to acknowledge the event.
 * </p>
 */
public class RestartHandler extends StateHandler<StateMachine.State, Context> {

	private Timeout startTimeout;
	private RestartState restartState;

	@Override
	protected void onEntry(Context context) {
		this.restartState = RestartState.TRYING_TO_STOP;
	}

	@Override
	public StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final var inverter = context.getParent();

		// The inverter is already started
		inverter._setStartStop(StartStop.START);

		switch (this.restartState) {
		case TRYING_TO_STOP -> {

			// As the inverter is not stopping until the external condition is cleared, we
			// do not use a timeout here.
			setValue(inverter, INVERTER_RESTART_STOPPING, true);
			inverter.setRequestedState(KacoSunSpecModel.S64201.S64201RequestedState.OFF);

			if (!inverter.hasFailure() && inverter.isShutdown()) {
				setValue(inverter, INVERTER_RESTART_STOPPING, false);
				this.restartState = RestartState.TRYING_TO_START;
			}
		}

		case TRYING_TO_START -> {
			if (this.startTimeout == null) {
				this.startTimeout = Timeout.of(Context.TIMEOUT);
				this.startTimeout.start(context.clock);
			}

			if (this.startTimeout.elapsed(context.clock)) {
				inverter._setMaxStartTimeout(true);
				return StateMachine.State.ERROR;
			}

			inverter.setRequestedState(KacoSunSpecModel.S64201.S64201RequestedState.GRID_CONNECTED);
			setValue(inverter, INVERTER_RESTART_STARTING, true);

			if (inverter.isRunning()) {
				setValue(inverter, INVERTER_RESTART_STARTING, false);
				return StateMachine.State.RUNNING;
			}
		}
		}

		return StateMachine.State.RESTART;
	}

	@Override
	protected void onExit(Context context) {
		final var inverter = context.getParent();
		setValue(inverter, INVERTER_RESTART_STOPPING, false);
		setValue(inverter, INVERTER_RESTART_STARTING, false);
		this.startTimeout = null;
	}

	private enum RestartState {
		TRYING_TO_STOP, //
		TRYING_TO_START
	}
}