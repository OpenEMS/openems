package io.openems.edge.lucidcontrol.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.lucidcontrol.api.AbstractLucidControlBridgeTask;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridgeTask;
import io.openems.edge.common.component.ComponentManager;

/**
 * This task will be added to the LucidControl Bridge. It gets the the Voltage of a Device and put it into the Channel.
 * Same goes for the pressure channel.
 */
public class LucidControlInputTask extends AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    private final ChannelAddress barAddress;
    private final ChannelAddress voltageAddress;

    private final String path;
    private final String voltage;
    private final int pinPos;
    //will be changed, just a placeholder atm
    private final double maxPressure;
    private int maxVoltage;


    public LucidControlInputTask(String moduleId, String deviceId, String path, String voltage, int pinPos,
                                 double maxPressure, ChannelAddress barAddress, ChannelAddress voltageAddress, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        super(moduleId, cpm);

        this.path = path;
        this.voltage = voltage;
        this.pinPos = pinPos;
        this.allocateMaxVoltage();
        this.maxPressure = maxPressure;
        this.barAddress = barAddress;
        this.voltageAddress = voltageAddress;
    }

    /**
     * Allocates the maxVoltage, depending on the Config of the LucidControlInputDevice.
     */
    private void allocateMaxVoltage() {
        this.maxVoltage = Integer.parseInt(this.voltage.replaceAll("\\D+", ""));
    }

    /**
     * Gets the CommandLine result of the Bridge and calculates pressure.
     *
     * @param voltageRead result of the Bridge command line.
     */
    @Override
    public void setResponse(double voltageRead) {
        try {
            super.cpm.getChannel(this.voltageAddress).setNextValue(voltageRead);
            super.cpm.getChannel(this.barAddress).setNextValue((voltageRead * this.maxPressure) / this.maxVoltage);
        } catch (OpenemsError.OpenemsNamedException e) {
            super.log.warn("Couldn't write into own Channel. Couldn't found the id/ChannelId");
        }
    }

    /**
     * Path of the LucidControlModule.
     *
     * @return the path.
     */
    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean isWriteDefined() {
        return false;
    }


    /**
     * Pin Position of the Device.
     *
     * @return pinPosition.
     */
    public int getPinPos() {
        return this.pinPos;
    }

    /**
     * Returns the Request String to read from this device.
     */
    @Override
    public String getRequest() {
        return " -tV -c" + this.pinPos + " -r";
    }

    @Override
    public boolean isRead() {
        return true;
    }
}
