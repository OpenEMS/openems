package io.openems.edge.bridge.rest.communcation.api;

import org.osgi.service.cm.ConfigurationException;

import java.util.Map;

public interface RestBridge {
    /**
     * Adds the RestRequest to the tasks map.
     *
     * @param id      identifier == remote device Id usually from Remote Device config
     * @param request the RestRequest created by the Remote Device.
     * @throws ConfigurationException if the id is already in the Map.
     */

    void addRestRequest(String id, RestRequest request) throws ConfigurationException;

    void removeRestRemoteDevice(String deviceId);

    RestRequest getRemoteRequest(String id);

    Map<String, RestRequest> getAllRequests();

    boolean connectionOk();
}
