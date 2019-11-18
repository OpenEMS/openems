package io.openems.edge.bridgei2c.task;

import io.openems.edge.common.channel.WriteChannel;

public abstract class I2cTask {

    private String pwmModuleId;

    public I2cTask(String pwmModuleId){
        this.pwmModuleId = pwmModuleId;
    }
    //TODO Do correct stuff in future
        public abstract void doStuff();

    public abstract WriteChannel<Float> powerLevel();
}
