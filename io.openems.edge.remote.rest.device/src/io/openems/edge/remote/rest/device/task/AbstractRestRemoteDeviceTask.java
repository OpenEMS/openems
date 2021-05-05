package io.openems.edge.remote.rest.device.task;

import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;

/**
 * This class hold the general information a RestTask has.
 */
public abstract class AbstractRestRemoteDeviceTask implements RestRequest {

    private final String remoteDeviceId;
    private final String deviceChannel;
    private final String realDeviceId;
    private final Logger logger;
    private final ComponentManager cpm;


    AbstractRestRemoteDeviceTask(String remoteDeviceId, String realDeviceId, String deviceChannel, Logger log, ComponentManager cpm) {
        this.remoteDeviceId = remoteDeviceId;
        this.deviceChannel = deviceChannel;
        this.realDeviceId = realDeviceId;
        this.logger = log;
        this.cpm = cpm;
    }

    /**
     * Returns the Request (ComponentId/ChannelId). Usually called by the
     * {@link io.openems.edge.bridge.communication.remote.rest.RestBridgeImpl}.
     *
     * @return The ChannelAddress String.
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

    protected ComponentManager getCpm() {
        return this.cpm;
    }
}
