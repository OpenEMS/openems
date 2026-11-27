package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;

public class StateMachine extends AbstractStateMachine<EvseSingleState, Context> {

	public StateMachine(EvseSingleState initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<EvseSingleState, Context> getStateHandler(EvseSingleState state) {
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case EV_NOT_CONNECTED -> new EvNotConnectedHandler();
		case NOT_READY -> new NotReadyHandler();
		case EV_CONNECTED -> new EvConnectedHandler();
		case CHARGE_PAUSED -> new ChargePausedHandler();
		case CHARGE_DISABLED -> new ChargeDisabledHandler();
		case CHARGE_RESUMING -> new ChargeResumingHandler();
		case CHARGING -> new ChargingHandler();
		case FINISHED_EV_STOP -> new FinishedEvStopHandler();
		case FINISHED_ENERGY_SESSION_LIMIT -> new FinishedEnergySessionLimitHandler();
		case PHASE_SWITCH_TO_THREE_PHASE -> new PhaseSwitchHandler.ToThreePhase();
		case PHASE_SWITCH_TO_SINGLE_PHASE -> new PhaseSwitchHandler.ToSinglePhase();
		};
	}
}