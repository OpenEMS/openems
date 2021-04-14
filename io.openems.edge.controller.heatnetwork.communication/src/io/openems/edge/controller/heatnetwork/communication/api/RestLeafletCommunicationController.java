package io.openems.edge.controller.heatnetwork.communication.api;

import java.util.List;
import java.util.Map;

public interface RestLeafletCommunicationController extends LeafletCommunicationController {

    Map<Integer, List<RestRequest>> getAllRequests();

    RestRequestManager getRestManager();

    /**
     * Adds RestRequests to allRequests. This is important for the Manager handling the Requests.
     *
     * @param additionalRequests the additional Requests.
     */
    void addRestRequests(Map<Integer, List<RestRequest>> additionalRequests);
}
