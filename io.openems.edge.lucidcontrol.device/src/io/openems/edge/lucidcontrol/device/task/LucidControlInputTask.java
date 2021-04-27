package io.openems.edge.lucidcontrol.device.task;

import io.openems.edge.bridge.lucidcontrol.api.AbstractLucidControlBridgeTask;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridgeTask;
import io.openems.edge.common.channel.Channel;

public class LucidControlInputTask extends AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    private Channel<Double> barChannel;
    private Channel<Double> voltageChannel;
    private String path;
    private String voltage;
    private int pinPos;
    private double lastBarValue = 0;
    //will be changed, just a placeholder atm
    private double maxPressure;
    //max Voltage is needed later depending on module and device; atm we just need 10V
    private int maxVoltage;


    public LucidControlInputTask(String moduleId, String deviceId, String path, String voltage, int pinPos,
                                 double maxPressure, Channel<Double> barChannel, Channel<Double> voltageChannel) {
        super(moduleId, deviceId);

        this.barChannel = barChannel;
        this.path = path;
        this.voltage = voltage;
        this.pinPos = pinPos;
        allocateMaxVoltage();
        this.maxPressure = maxPressure;
        this.voltageChannel = voltageChannel;
    }

    private void allocateMaxVoltage() {

        maxVoltage = Integer.parseInt(voltage.replaceAll("\\D+", ""));

    }

    /**
     * Gets the CommandLine result of the Bridge and calculates pressure.
     *
     * @param voltageRead result of the Bridge command line.
     */
    @Override
    public void setResponse(double voltageRead) {

        this.voltageChannel.setNextValue(voltageRead);
        double percent = (voltageRead * 100) / maxVoltage;
        this.barChannel.setNextValue((voltageRead * maxPressure) / maxVoltage);

    }

    /**
     * path of the LucidControModule.
     *
     * @return the path.
     */
    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean writeTaskDefined() {
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
