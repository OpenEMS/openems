package io.openems.edge.pwmDevice.task;

import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.common.channel.WriteChannel;

public class PwmDeviceTask extends I2cTask {

    private WriteChannel<Float> powerLevel;


    public PwmDeviceTask(String pwmModuleId, WriteChannel<Float> powerLevel) {
        super(pwmModuleId);
        this.powerLevel = powerLevel;
    }


    @Override
    public void doStuff() {
        
    }

    @Override
    public WriteChannel<Float> powerLevel() {
        return null;
    }
}
