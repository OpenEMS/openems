package io.openems.edge.controller.heatnetwork.communication.api;


import io.openems.edge.remote.rest.device.api.RestRemoteDevice;

public interface RestRequest extends Request {
    RestRemoteDevice getRequest();

    RestRemoteDevice getCallbackRequest();

    RequestType getRequestType();



}
