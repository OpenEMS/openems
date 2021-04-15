package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.edge.heatsystem.components.Valve;
import org.joda.time.DateTime;

public class ValveLineHeater extends AbstractLineHeater {

    private Valve valve;


    public ValveLineHeater(boolean booleanControlled, Valve valve) {
        super(booleanControlled);
        this.valve = valve;
    }

    @Override
    public boolean startHeating() {
        double lastPower = this.valve.getLastPowerLevel().value().isDefined() ? this.valve.getLastPowerLevel().value().get() : DEFAULT_LAST_POWER_VALUE;
        if (this.isRunning == false || this.valve.isChanging() == false || lastPower < LAST_POWER_CHECK_VALUE) {
            this.isRunning = true;
            //Either fullpower set .--> ValveManager handles OR you could change Valve directly
            //this.valveBypass.setPowerLevelPercent().setNextValue((FULL_POWER));
            if (super.isBooleanControlled()) {
                this.valve.changeByPercentage(100);
            } else {
                this.valve.changeByPercentage(FULL_POWER - lastPower);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean stopHeating(DateTime lifecycle) {
        double lastPower;
        if (super.isBooleanControlled()) {
            lastPower = 100;
        } else {
            lastPower = this.valve.getLastPowerLevel().value().isDefined() ? this.valve.getLastPowerLevel().value().get() : DEFAULT_LAST_POWER_VALUE;
        }
        if (this.isRunning || this.valve.isChanging() == false || lastPower > LAST_POWER_CHECK_VALUE) {
            this.isRunning = false;
            this.valve.changeByPercentage(-lastPower);
            this.setLifeCycle(lifecycle);
            return true;
        }
        return false;
    }
}
