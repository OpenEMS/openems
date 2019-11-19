package io.openems.edge.bridgei2c.task;

import io.openems.edge.common.channel.WriteChannel;

public abstract class I2cTaskImpl implements I2cTask {

    private String pwmModuleId;

    public I2cTaskImpl(String pwmModuleId) {
        this.pwmModuleId = pwmModuleId;
    }

    @Override
    public String getPwmModuleId() {
        return pwmModuleId;
    }
}
