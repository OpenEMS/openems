package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

    @Override
    public State runAndGetNextState(Context context) {
        final var inverter = context.getParent();
        return switch (inverter.getStartStopTarget()) {
            case UNDEFINED ->
                // Stuck in UNDEFINED State
                State.UNDEFINED;

            case START -> {
                // force START
                if (inverter.hasFaults()) {
                    // Has Faults -> error handling
                    yield State.ERROR;
                } else {
                    // No Faults -> start
                    yield State.GO_RUNNING;
                }
            }
            case STOP ->
                // force STOP
                State.GO_STOPPED;
        };
    }
}
