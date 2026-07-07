package io.openems.edge.controller.ess.sohcycle.statemachine;

public class ReferenceCycleDischargingWaitHandler extends AbstractWaitHandler {

    @Override
    protected StateMachine.State getNextStateAfterWait() {
		return StateMachine.State.MEASUREMENT_CYCLE_CHARGING;
    }

    @Override
    protected StateMachine.State getCurrentState() {
        return StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT;
    }
}
