package io.openems.edge.controller.heatnetwork.communication.api;

import java.util.List;
import java.util.Map;

public interface RestRequestManager extends RequestManager {
    /**
     * Usually Called By CommunicationMaster.
     * Give all of the Requests and handle them by ManageType (e.g. FIFO) and Maximum Requests.
     * Includes a Waitlist
     *
     * @param allRequests All Requests mapped by Integer (Usually a Position/Number)
     */
    void manageRequests(Map<Integer, List<RestRequest>> allRequests);

    /**
     * returns the managed Request either all of them (e.g. if isForcing in Communicationmaster is set) or
     * returns managed requests (Maximum Requests size determined by Keys of the Map).
     * @return the ManagedRequests.
     */

    Map<Integer, List<RestRequest>> getManagedRequests();

    /**
     * Stop the Request by Key and Value.
     *
     * @param key          key of request map
     * @param restRequests requests to stop/Callback to 0.
     */
    void stopRestRequests(Integer key, List<RestRequest> restRequests);
}
