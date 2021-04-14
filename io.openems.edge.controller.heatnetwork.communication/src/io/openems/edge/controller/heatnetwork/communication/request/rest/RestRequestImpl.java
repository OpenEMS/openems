package io.openems.edge.controller.heatnetwork.communication.request.rest;


import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.api.RequestType;
import io.openems.edge.rest.remote.device.general.api.RestRemoteDevice;
import org.osgi.service.cm.ConfigurationException;

/**
 * Rest Request Handling a request and callback RestRemoteDevice.
 */
public class RestRequestImpl implements RestRequest {
    //Request input
    private RestRemoteDevice request;
    //Write to/Signal/Callback/Allow
    private RestRemoteDevice callback;

    private RequestType requestType;


    public RestRequestImpl(RestRemoteDevice request, RestRemoteDevice callback, RequestType type) throws ConfigurationException {
        if (request.isRead()) {
            this.request = request;
            this.requestType = type;
        } else {
            throw new ConfigurationException("Request RemoteDevice is not a Read Task", request.getId());
        }
        if (callback.isWrite()) {
            this.callback = callback;
        } else {
            throw new ConfigurationException("Request RemoteDevice is not a Write Task", callback.getId());
        }
    }


    @Override
    public RestRemoteDevice getRequest() {
        return this.request;
    }

    @Override
    public RestRemoteDevice getCallbackRequest() {
        return this.callback;
    }

    @Override
    public RequestType getRequestType() {
        return this.requestType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof RestRequestImpl) {
            RestRequestImpl otherObject = (RestRequestImpl) o;
            return otherObject.getRequest().equals(this.getRequest())
                    && otherObject.getCallbackRequest().equals(this.getCallbackRequest())
                    && otherObject.getRequestType().equals(this.requestType);
        }
        return false;


    }
}
