package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;

public class ErrorAbortHandler extends StateHandler<StateMachine.State, Context> {
    private static final Logger log = LoggerFactory.getLogger(ErrorAbortHandler.class);

    @Override
    protected StateMachine.State runAndGetNextState(Context context) {
        final int soc = context.ess.getSoc().orElse(0);
        context.logWarn(log, String.format("%s: SoC=%d%%, SoH cycle aborted due to error",
                StateMachine.State.ERROR_ABORT.getName(), soc));

        if (context.isManualModeOff()) {
            context.logWarn(log, "Controller is already in MANUAL OFF mode.");
            return StateMachine.State.IDLE;
        }
        // TODO(alex.belke 19.01.2026): add error handling
        return StateMachine.State.ERROR_ABORT;
    }
}
