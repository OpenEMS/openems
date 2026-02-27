package io.openems.edge.controller.ess.sohcycle.statemachine;

import io.openems.common.exceptions.OpenemsError;

public class MeasurementCycleDischargingWaitHandler extends AbstractWaitHandler {

    @Override
    protected StateMachine.State getNextStateAfterWait() {
        return StateMachine.State.EVALUATE_RESULT;
    }

    @Override
    protected StateMachine.State getCurrentState() {
        return StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT;
    }

    @Override
    protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
        context.refreshMeasurementChargingVoltageRange();
        return super.runAndGetNextState(context);
    }
}
