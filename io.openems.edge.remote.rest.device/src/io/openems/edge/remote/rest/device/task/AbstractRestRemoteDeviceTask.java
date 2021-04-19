package io.openems.edge.remote.rest.device.task;

import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import org.slf4j.Logger;

public abstract class AbstractRestRemoteDeviceTask implements RestRequest {

    private final String remoteDeviceId;
    private final String deviceChannel;
    private final String realDeviceId;
    private final Logger logger;


    AbstractRestRemoteDeviceTask(String remoteDeviceId, String realDeviceId, String deviceChannel, Logger log) {
        this.remoteDeviceId = remoteDeviceId;
        this.deviceChannel = deviceChannel;
        this.realDeviceId = realDeviceId;
        this.logger = log;
    }

    /**
     * Returns the Request. (ComponentId/ChannelId) For the Bridge.
     *
     * @return the ComponentId/ChannelId String.
     */
    @Override
    public String getRequest() {
        return this.realDeviceId + "/" + this.deviceChannel;
    }

    /**
     * Gets the Id of the Configured Component.
     *
     * @return the Id String.
     */
    @Override
    public String getDeviceId() {
        return this.remoteDeviceId;
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
