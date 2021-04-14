package io.openems.edge.rest.remote.device.general.api;

public interface RestRemoteDevice {

    boolean setValue(String value);

    //returns the Correct value as a string
    String getValue();

    String getType();

    boolean setAllowRequest(boolean allow);

    String getRemoteUnit();

    String getId();

    boolean isWrite();

    boolean isRead();

    boolean connectionOk();
}
