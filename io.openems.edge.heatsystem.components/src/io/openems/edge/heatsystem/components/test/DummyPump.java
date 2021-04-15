package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.heatsystem.components.PassingChannel;
import io.openems.edge.pwm.device.api.PwmPowerLevelChannel;
import io.openems.edge.relays.device.api.ActuatorRelaysChannel;


public class DummyPump extends AbstractOpenemsComponent implements OpenemsComponent, Pump {

    private ActuatorRelaysChannel relays;
    //private PwmPowerLevelChannel pwm;
    private boolean isRelays = false;
    private boolean isPwm = false;
    private PwmPowerLevelChannel pwm;

    public DummyPump(String id, ActuatorRelaysChannel relays, PwmPowerLevelChannel pwm, String type) {
        super(OpenemsComponent.ChannelId.values(), PassingChannel.ChannelId.values());

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

        this.getIsBusy().setNextValue(false);
        this.getPowerLevel().setNextValue(0);
        this.getLastPowerLevel().setNextValue(0);

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
                if (this.getPowerLevel().getNextValue().get() + percentage < 0) {
                    controlRelays(false, "");
                    System.out.println("Set Next WriteValue to 0.f");
                    this.getPowerLevel().setNextValue(0);
                    return true;
                }
            } else if (percentage <= 0) {
                controlRelays(false, "");
            } else {
                controlRelays(true, "");
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            this.getLastPowerLevel().setNextValue(this.getPowerLevel().getNextValue().get());
            currentPowerLevel = this.getPowerLevel().getNextValue().get();
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100 : currentPowerLevel;
            currentPowerLevel = currentPowerLevel < 0 ? 0 : currentPowerLevel;
            System.out.println("Set Next Write Value to " + currentPowerLevel + "in " + pwm.id());
            this.getPowerLevel().setNextValue(currentPowerLevel);
        }
        return true;
    }

    @Override
    public void controlRelays(boolean activate, String whichRelays) {
        if (this.relays.isCloser().getNextValue().get()) {
            System.out.println("Relays is " + activate);
        } else {
            System.out.println("Relays is " + !activate);
        }
    }

    @Override
    public void setPowerLevel(double percent) {
        this.changeByPercentage(percent - this.getCurrentPowerLevelValue());
    }
}
