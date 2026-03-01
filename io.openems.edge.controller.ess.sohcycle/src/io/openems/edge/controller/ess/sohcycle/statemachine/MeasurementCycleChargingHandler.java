package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class MeasurementCycleChargingHandler extends StateHandler<StateMachine.State, Context> {
    private static final Logger log = LoggerFactory.getLogger(MeasurementCycleChargingHandler.class);

    @Override
    protected void onEntry(Context context) throws OpenemsError.OpenemsNamedException {
        super.onEntry(context);
        context.resetMeasurementChargingMaxMinVoltage();
    }

    @Override
    protected StateMachine.State runAndGetNextState(Context context) {
        context.refreshMeasurementChargingVoltageRange();

        var result = context.applyChargingTarget(EssSohCycleConstants.MAX_SOC);

        if (result == null) {
            return StateMachine.State.MEASUREMENT_CYCLE_CHARGING;
        }

        if (!result.thresholdReached()) {
            context.logPowerState(log, StateMachine.State.MEASUREMENT_CYCLE_CHARGING, result,
                    EssSohCycleConstants.MAX_SOC, true);
            return StateMachine.State.MEASUREMENT_CYCLE_CHARGING;
        }

        return StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT;
    }
}
