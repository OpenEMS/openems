package io.openems.edge.lucidcontrol.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.lucidcontrol.api.AbstractLucidControlBridgeTask;
import io.openems.edge.lucidcontrol.api.LucidControlBridgeTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A LucidControlOutputTask. This allows to write an Output with the help of the LucidControlBridge.
 * The Output depends on the percentChannel given to the task and the max voltage possible.
 */
public class LucidControlOutputTask extends AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    private final ChannelAddress percentAddress;
    private final String path;
    private final Integer voltage;
    private final int pinPos;
    private int maxVoltage;
    private final Map<Double, Double> voltageThresholdMap;
    private final List<Double> keyList;

    public LucidControlOutputTask(String moduleId, String deviceId, String path, Integer voltage, int pinPos,
                                  ChannelAddress percentAddress, List<Double> keyList, Map<Double, Double> voltageThresholdMap, ComponentManager cpm) {
        super(moduleId, cpm);

        this.percentAddress = percentAddress;
        this.path = path;
        this.voltage = voltage;
        this.pinPos = pinPos;
        this.allocateMaxVoltage();
        this.voltageThresholdMap = voltageThresholdMap;
        this.keyList = keyList;
    }

    private void allocateMaxVoltage() {
        this.maxVoltage = this.voltage;

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
        try {
            return this.cpm.getChannel(this.percentAddress).value().isDefined();
        } catch (OpenemsError.OpenemsNamedException e) {
            super.log.warn("Couldn't read Channel of " + this.percentAddress);
            return false;
        }
    }

    /**
     * Returns the Request String to write to a device.
     */
    @Override
    public String getRequest() {
        return " -w" + this.calculateVoltage() + " -c" + this.pinPos + " -tV";
    }

    /**
     * Calculates the Voltage to write to the Request.
     *
     * @return the output Voltage.
     */
    private double calculateVoltage() {
        double adaptedVoltage;
        try {
            adaptedVoltage = this.calculateAdaptedVoltage();
        } catch (OpenemsError.OpenemsNamedException e) {
            super.log.warn("Couldn't read percent Channel " + this.percentAddress);
            adaptedVoltage = 0.d;
        }
        return adaptedVoltage * this.maxVoltage / 100;
    }

    /**
     * Calculates an Adapted Voltage by reading the Percent Channel / SetPoint and convert it to a Voltage value.
     * And adding/Subtracting the ThresholdValue defined by the Configuration of the LucidControlOutputDevice.
     *
     * @return the adapted Voltage value
     * @throws OpenemsError.OpenemsNamedException if the Channel cannot be found.
     */
    private double calculateAdaptedVoltage() throws OpenemsError.OpenemsNamedException {
        Channel<?> percentChannel = this.cpm.getChannel(this.percentAddress);
        double percentChannelValue = percentChannel.value().isDefined() ? (Double) percentChannel.value().get() : 0;
        double percentToVoltage = percentChannelValue >= 0 ? percentChannelValue : 0;
        if (this.keyList.size() > 0) {
            AtomicBoolean wasSet = new AtomicBoolean();
            wasSet.set(false);
            AtomicInteger keyInList = new AtomicInteger();
            keyInList.set(-1);
            this.keyList.forEach(key -> {
                if (key <= percentToVoltage) {
                    if (wasSet.get()) {
                        if (key > this.keyList.get(keyInList.get())) {
                            keyInList.set(this.keyList.indexOf(key));
                        }
                    } else {
                        wasSet.set(true);
                        keyInList.set(this.keyList.indexOf(key));
                    }
                }
            });
            if (wasSet.get()) {
                return percentToVoltage + this.voltageThresholdMap.get(this.keyList.get(keyInList.get()));
            }
        }

        return percentToVoltage;
    }

    /**
     * Tells the Bridge if this Task is a Read Task.
     *
     * @return true if readTask.
     */

    @Override
    public boolean isRead() {
        return false;
    }


}
