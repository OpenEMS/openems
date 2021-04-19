package io.openems.edge.bridge.communication.remote.rest.api;

public interface RestRequest {

    /**
     * Returns the Request. (ComponentId/ChannelId) For the Bridge.
     * @return the ComponentId/ChannelId String.
     */
    String getRequest();

    /**
     * Gets the Id of the Configured Component.
     * @return the Id String.
     */
    String getDeviceId();

}
