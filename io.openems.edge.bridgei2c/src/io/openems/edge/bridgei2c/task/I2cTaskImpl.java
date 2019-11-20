package io.openems.edge.bridgei2c.task;

public abstract class I2cTaskImpl implements I2cTask {

    private String pwmModuleId;
    private String deviceId;

    public I2cTaskImpl(String pwmModuleId, String deviceId) {
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
