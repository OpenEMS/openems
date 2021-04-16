package io.openems.edge.bridge.communication.remote.rest.api;

public interface RestRequest {
    //Return DeviceId + Channel
    String getRequest();

    String getDeviceId();

    String getRealDeviceId();

    String getDeviceType();

    boolean unitWasSet();

    /**
     * Sets the Unit for a Read or Write Task.
     *
     * @param succ   Success of the REST GET Request for Unit.
     * @param answer complete GET String. Will be Split at "Unit".
     */

    void setUnit(boolean succ, String answer);


}
