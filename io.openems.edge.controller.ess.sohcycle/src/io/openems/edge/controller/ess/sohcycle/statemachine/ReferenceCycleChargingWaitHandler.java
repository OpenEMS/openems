package io.openems.edge.controller.ess.sohcycle.statemachine;

public class ReferenceCycleChargingWaitHandler extends AbstractWaitHandler {

    @Override
    protected StateMachine.State getNextStateAfterWait() {
		return StateMachine.State.REFERENCE_CYCLE_DISCHARGING;
    }

    @Override
    protected StateMachine.State getCurrentState() {
        return StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT;
    }
}
