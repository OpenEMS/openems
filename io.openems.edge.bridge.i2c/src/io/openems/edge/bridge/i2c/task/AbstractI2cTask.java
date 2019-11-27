package io.openems.edge.bridge.i2c.task;

public abstract class AbstractI2cTask implements I2cTask {

    private String pwmModuleId;
    private String deviceId;

    public AbstractI2cTask(String pwmModuleId, String deviceId) {
        this.pwmModuleId = pwmModuleId;
        this.deviceId = deviceId;
    }

    @Override
    public String getPwmModuleId() {
        return pwmModuleId;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }
}
