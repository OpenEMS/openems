package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.Utils;

public class EvaluateResultHandler extends StateHandler<StateMachine.State, Context> {
    private static final Logger log = LoggerFactory.getLogger(EvaluateResultHandler.class);

    @Override
    protected StateMachine.State runAndGetNextState(Context context) {
        final var controller = context.getParent();
        final int soc = context.ess.getSoc().orElse(0);

        final Long startWh = context.getMeasurementStartEnergyWh();
        final var endWhValue = context.ess.getActiveDischargeEnergy();

        if (startWh == null || !endWhValue.isDefined()) {
            context.logError(log, String.format(
                    "%s: Missing measurement data (startWh=%s, endWhDefined=%s). Aborting.",
                    StateMachine.State.EVALUATE_RESULT.getName(),
                    startWh, endWhValue.isDefined()
            ));
            return StateMachine.State.ERROR_ABORT;
        }

        final long endWh = endWhValue.get();
        final long measuredCapacityWh = endWh - startWh;

        if (measuredCapacityWh <= 0) {
            context.logError(log, String.format(
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
        var sohResult = context.calculateSoh(measuredCapacityWh);
        if (sohResult.isEmpty()) {
            context.logError(log, String.format(
                    "%s: SoH calculation failed. Aborting.",
                    StateMachine.State.EVALUATE_RESULT.getName()
            ));
            return StateMachine.State.ERROR_ABORT;
        }

        var result = sohResult.get();
		setValue(controller, ControllerEssSohCycle.ChannelId.SOH_PERCENT, result.soh());
		setValue(controller, ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG, Utils.round2(result.sohRaw()));
		setValue(controller, ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, measuredCapacityWh);
		setValue(controller, ControllerEssSohCycle.ChannelId.IS_MEASURED, true);
        return StateMachine.State.DONE;
    }

}
