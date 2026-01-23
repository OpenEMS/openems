package io.openems.edge.controller.ess.sohcycle.statemachine;

public class MeasurementCycleChargingWaitHandler extends AbstractWaitHandler {

    @Override
    protected StateMachine.State getNextStateAfterWait() {
		return StateMachine.State.CHECK_BALANCING;
    }

    @Override
    protected StateMachine.State getCurrentState() {
        return StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT;
    }
}
