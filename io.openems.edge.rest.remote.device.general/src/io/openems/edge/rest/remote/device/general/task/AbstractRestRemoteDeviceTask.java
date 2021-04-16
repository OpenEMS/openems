package io.openems.edge.rest.remote.device.general.task;

import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import io.openems.edge.common.channel.Channel;

public abstract class AbstractRestRemoteDeviceTask implements RestRequest {

    private String remoteDeviceId;
    private String deviceChannel;
    private String realDeviceId;
    private String deviceType;
    private boolean unitWasSet;
    private Channel<String> unit;

    AbstractRestRemoteDeviceTask(String remoteDeviceId, String realDeviceId, String deviceChannel,
                                 String deviceType, Channel<String> unit) {
        this.remoteDeviceId = remoteDeviceId;

        this.deviceChannel = deviceChannel;
        this.unit = unit;
        this.realDeviceId = realDeviceId;
        this.deviceType = deviceType;
    }

    @Override
    public String getRequest() {
        return this.realDeviceId + "/" + this.deviceChannel;
    }

    @Override
    public String getDeviceId() {
        return this.remoteDeviceId;
    }


    @Override
    public String getDeviceType() {
        return this.deviceType;
    }

    @Override
    public String getRealDeviceId() {
        return this.realDeviceId;
    }


    /**
     * Sets the Unit for a Read or Write Task.
     *
     * @param succ   Success of the REST GET Request for Unit.
     * @param answer complete GET String. Will be Split at "Unit".
     */
    @Override
    public void setUnit(boolean succ, String answer) {
        if (succ && !this.unitWasSet) {
            if (answer.contains("Unit")) {
                String[] parts = answer.split("\"Unit\"");
                if (parts[1].contains("\"")) {

                    String newParts = parts[1].substring(parts[1].indexOf("\""), parts[1].indexOf("\"", parts[1].indexOf("\"") + 1));
                    newParts = newParts.replace("\"", "");
                    this.unit.setNextValue(newParts);
                    this.unitWasSet = true;
                }
            } else {
                this.unit.setNextValue("");
                this.unitWasSet = true;
            }
        }
    }

    @Override
    public boolean unitWasSet() {
        return this.unitWasSet;
    }
}
