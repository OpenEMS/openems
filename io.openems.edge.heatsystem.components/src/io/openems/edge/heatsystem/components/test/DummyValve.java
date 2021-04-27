package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.PassingChannel;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.relay.api.Relay;


public class DummyValve extends AbstractOpenemsComponent implements Valve, OpenemsComponent {
    /*
     * This Class works the same way as the ValveImpl except it's constructor and lacking of the
     * Osgi Activate and Deactivate Methods for obv Reasons. It should be combined with the Passing Station
     * + the DummyPump.
     * */

    private Relay opens;
    private Relay closing;
    private double secondsPerPercentage;
    private boolean percentageWasSet = false;
    private long timeStampValve;


    public DummyValve(Relay valveOpen, Relay valveClose, String id, double valveTimeInSeconds) {
        super(OpenemsComponent.ChannelId.values(), PassingChannel.ChannelId.values());
        super.activate(null, id, "", true);
        this.opens = valveOpen;
        this.closing = valveClose;
        this.secondsPerPercentage = valveTimeInSeconds / 100.d;
        this.getLastPowerLevel().setNextValue(0);
        this.getPowerLevel().setNextValue(0);
        this.getTimeNeeded().setNextValue(0);
        this.getIsBusy().setNextValue(false);
    }


    private void valveClose() {
        if (!this.getIsBusy().getNextValue().get()) {
            controlRelays(false, "Open");
            controlRelays(true, "Closed");
            this.getIsBusy().setNextValue(true);
            timeStampValve = System.currentTimeMillis();
        }
    }


    private void valveOpen() {
        //opens will be set true when closing is done
        if (!this.getIsBusy().getNextValue().get()) {
            controlRelays(false, "Closed");
            controlRelays(true, "Open");
            this.getIsBusy().setNextValue(true);
            timeStampValve = System.currentTimeMillis();
        }
    }


    @Override
    public boolean readyToChange() {
//        if (percentageWasSet) {
//            if ((System.currentTimeMillis() - timeStampValve)
//                    >= ((this.getTimeNeeded().getNextValue().get() * 1000))) {
//                percentageWasSet = false;
//                return true;
//            }
//        }
//        return false;
        return System.currentTimeMillis() - timeStampValve >= (this.getTimeNeeded().getNextValue().get() * 1000);
    }

    @Override
    public boolean changeByPercentage(double percentage) {
        double currentPowerLevel;

        //opens / closes valve by a certain percentage value
        if ((this.getIsBusy().getNextValue().get()) || percentage == 0) {
            return false;
        } else {
            currentPowerLevel = this.getPowerLevel().getNextValue().get();
            this.getLastPowerLevel().setNextValue(currentPowerLevel);
            currentPowerLevel += percentage;

            currentPowerLevel = currentPowerLevel > 100 ? 100
                    : currentPowerLevel < 0 ? 0 : currentPowerLevel;

            this.getPowerLevel().setNextValue(currentPowerLevel);
            System.out.println("Next PowerLevel Value is " + currentPowerLevel);
            if (Math.abs(percentage) >= 100) {
                this.getTimeNeeded().setNextValue(100 * secondsPerPercentage);
            } else {
                this.getTimeNeeded().setNextValue(Math.abs(percentage) * secondsPerPercentage);
            }
            if (percentage < 0) {
                controlRelays(false, "Open");
                valveClose();
            } else {
                controlRelays(false, "Closed");
                valveOpen();
            }
            percentageWasSet = true;
            return true;
        }
    }


    public void controlRelays(boolean activate, String whichRelays) {
        switch (whichRelays) {
            case "Open":
                if (this.opens.isCloser().getNextValue().get()) {
                    System.out.println(activate);
                } else {
                    System.out.println(!activate);
                }
                break;
            case "Closed":
                if (this.closing.isCloser().getNextValue().get()) {
                    System.out.println(activate);
                } else {
                    System.out.println(!activate);
                }
                break;
        }
        if (!activate) {
            this.getIsBusy().setNextValue(false);
        }
    }


    // Added because of changes to valve interface. Won't compile otherwise.
    @Override
    public void forceOpen() {
    }

    @Override
    public void updatePowerLevel() {

    }

    @Override
    public boolean powerLevelReached() {
        return false;
    }

    @Override
    public boolean isChanging() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean shouldReset() {
        return false;
    }

    @Override
    public void forceClose() {
    }
}
