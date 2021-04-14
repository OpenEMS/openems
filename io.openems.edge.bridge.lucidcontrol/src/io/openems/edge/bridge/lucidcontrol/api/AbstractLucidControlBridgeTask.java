package io.openems.edge.bridge.lucidcontrol.api;


public abstract class AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    private String moduleId;
    private String deviceId;


    public AbstractLucidControlBridgeTask(String moduleId, String deviceId) {
        this.moduleId = moduleId;
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getModuleId() {
        return this.moduleId;
    }

}
