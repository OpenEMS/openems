package io.openems.edge.controller.heatnetwork.communication.api;


import io.openems.edge.rest.remote.device.general.api.RestRemoteDevice;

public interface RestRequest extends Request {
    RestRemoteDevice getRequest();

    RestRemoteDevice getCallbackRequest();

    RequestType getRequestType();



}
