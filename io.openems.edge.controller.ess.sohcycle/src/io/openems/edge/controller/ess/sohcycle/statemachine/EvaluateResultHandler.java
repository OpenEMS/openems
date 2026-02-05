package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;

public class EvaluateResultHandler extends StateHandler<StateMachine.State, Context> {
    private static final Logger log = LoggerFactory.getLogger(EvaluateResultHandler.class);

    @Override
    protected StateMachine.State runAndGetNextState(Context context) {
        final var controller = context.getParent();
        final int soc = context.ess.getSoc().orElse(0);

        final Long startWh = context.getMeasurementStartEnergyWh();
        final var endWhValue = context.ess.getActiveDischargeEnergy();

        if (startWh == null || !endWhValue.isDefined()) {
            context.logWarn(log, String.format(
                    "%s: Missing measurement data (startWh=%s, endWhDefined=%s). Aborting.",
                    StateMachine.State.EVALUATE_RESULT.getName(),
                    startWh, endWhValue.isDefined()
            ));
            return StateMachine.State.ERROR_ABORT;
        }

        final long endWh = endWhValue.get();
        final long measuredCapacityWh = endWh - startWh;

        if (measuredCapacityWh <= 0) {
            context.logWarn(log, String.format(
                    "%s: Invalid measured capacity (startWh=%d, endWh=%d, delta=%d). Aborting.",
                    StateMachine.State.EVALUATE_RESULT.getName(),
                    startWh, endWh, measuredCapacityWh
            ));
            return StateMachine.State.ERROR_ABORT;
        }

        context.logInfo(log, String.format(
                "%s: SoC=%d%%, measuredCapacity=%d Wh (start=%d, end=%d)",
                StateMachine.State.EVALUATE_RESULT.getName(),
                soc, measuredCapacityWh, startWh, endWh
        ));
        ChannelUtils.setValue(controller, ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, measuredCapacityWh);
        return StateMachine.State.DONE;
    }

}
