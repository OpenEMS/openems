package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.pwm.api.Pwm;
import io.openems.edge.relay.api.Relay;

/**
 * This Device acts as a Dummy for Unittests.
 */
public class DummyPump extends AbstractOpenemsComponent implements OpenemsComponent, Pump {

    private Relay relays;
    //private PwmPowerLevelChannel pwm;
    private boolean isRelays = false;
    private boolean isPwm = false;
    private Pwm pwm;

    public DummyPump(String id, Relay relays, Pwm pwm, String type) {
        super(OpenemsComponent.ChannelId.values(), HeatsystemComponent.ChannelId.values());

        super.activate(null, id, "", true);

        this.relays = relays;
        this.pwm = pwm;

        switch (type) {
            case "Relays":
                isRelays = true;
                break;

            case "Pwm":
                isPwm = true;
                break;

            default:
                isRelays = true;
                isPwm = true;
        }

        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);

    }

    @Override
    public boolean readyToChange() {
        return true;
    }


    /**
     * Like the original changeByPercentage just a bit adjusted.
     * @param percentage change the PowerLevel by this value.
     *
     * */
    @Override
    public boolean changeByPercentage(double percentage) {
        if (this.isRelays) {
            if (this.isPwm) {
                if (this.getPowerLevelChannel().getNextValue().get() + percentage < 0) {
                    controlRelays(false);
                    System.out.println("Set Next WriteValue to 0.f");
                    this.getPowerLevelChannel().setNextValue(0);
                    return true;
                }
            } else if (percentage <= 0) {
                controlRelays(false);
            } else {
                controlRelays(true);
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelChannel().getNextValue().get());
            currentPowerLevel = this.getPowerLevelChannel().getNextValue().get();
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100 : currentPowerLevel;
            currentPowerLevel = currentPowerLevel < 0 ? 0 : currentPowerLevel;
            System.out.println("Set Next Write Value to " + currentPowerLevel + "in " + pwm.id());
            this.getPowerLevelChannel().setNextValue(currentPowerLevel);
        }
        return true;
    }


    private void controlRelays(boolean activate) {
        if (this.relays.isCloser().getNextValue().get()) {
            System.out.println("Relays is " + activate);
        } else {
            System.out.println("Relays is " + !activate);
        }
    }

    @Override
    public void setPowerLevel(double percent) {
        this.changeByPercentage(percent - this.getPowerLevelValue());
    }
}
