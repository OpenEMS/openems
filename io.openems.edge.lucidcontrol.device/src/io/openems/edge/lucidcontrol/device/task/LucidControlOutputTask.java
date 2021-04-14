package io.openems.edge.lucidcontrol.device.task;

import io.openems.edge.bridge.lucidcontrol.task.AbstractLucidControlBridgeTask;
import io.openems.edge.bridge.lucidcontrol.task.LucidControlBridgeTask;
import io.openems.edge.common.channel.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LucidControlOutputTask extends AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    private Channel<Double> percentChannel;
    private String path;
    private String voltage;
    private int pinPos;
    private double lastVoltValue = 0;
    //will be changed, just a placeholder atm
    private double maxPressure;
    //max Voltage is needed later depending on module and device; atm we just need 10V
    private int maxVoltage;
    private Map<Double, Double> voltageThresholdMap;
    private List<Double> keyList;

    public LucidControlOutputTask(String moduleId, String deviceId, String path, String voltage, int pinPos,
                                  Channel<Double> percentChannel, List<Double> keyList, Map<Double, Double> voltageThresholdMap) {
        super(moduleId, deviceId);

        this.percentChannel = percentChannel;
        this.path = path;
        this.voltage = voltage;
        this.pinPos = pinPos;
        allocateMaxVoltage();
        this.voltageThresholdMap = voltageThresholdMap;
        this.keyList = keyList;
    }

    private void allocateMaxVoltage() {
        maxVoltage = Integer.parseInt(voltage.replaceAll("\\D+", ""));

    }

    @Override
    public void setResponse(double voltageRead) {
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
        return this.percentChannel.value().isDefined();
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
     * Returns the Request String to write to a device.
     */
    @Override
    public String getRequest() {
        return " -w" + calculateVoltage() + " -c" + this.pinPos + " -tV";
    }

    private double calculateVoltage() {

        double volt = calculateAdaptedVoltage() * maxVoltage / 100;
        this.lastVoltValue = volt;
        return volt;
    }

    private double calculateAdaptedVoltage() {
        double percent = this.percentChannel.value().get() > 0 ? this.percentChannel.value().get() : 0;
        if (this.keyList.size() > 0) {
            AtomicBoolean wasSet = new AtomicBoolean();
            wasSet.set(false);
            AtomicInteger keyInList = new AtomicInteger();
            keyInList.set(-1);
            this.keyList.forEach(key -> {
                if (key <= percent) {
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
                return percent + this.voltageThresholdMap.get(this.keyList.get(keyInList.get()));
            }
        }

        return percent;
    }

    @Override
    public boolean isRead() {
        return false;
    }


}
