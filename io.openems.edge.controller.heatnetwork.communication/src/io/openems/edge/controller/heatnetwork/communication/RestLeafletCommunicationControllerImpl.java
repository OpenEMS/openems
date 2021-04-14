package io.openems.edge.controller.heatnetwork.communication;

import io.openems.edge.controller.heatnetwork.communication.api.RestLeafletCommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.api.RequestManager;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequestManager;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.request.manager.RestRequestManagerImpl;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST Communication Controller handling multiple REST requests. Contains a RequestManager that organizes them by certain ManagingPattern(FIFO) etc.
 * Maximum Request given to Manager.
 * Possible to start/stop "Communication" or Autorun mode,
 * By executing Logic ---> Handle Manager --> Sort Requests etc.
 * Available: AllRequests/Current Requests
 */
public class RestLeafletCommunicationControllerImpl implements RestLeafletCommunicationController {

    private boolean start;
    private boolean isAutorun;

    private Map<Integer, List<RestRequest>> allRequests = new HashMap<>();
    private RestRequestManager requestManager;
    private final ConnectionType connectionType;

    public RestLeafletCommunicationControllerImpl(ConnectionType connectionType, ManageType manageType,
                                                  int maximumAllowedRequests, boolean forceHeating, boolean autoRun) throws ConfigurationException {
        this.connectionType = connectionType;
        if (connectionType.equals(ConnectionType.REST)) {
            this.requestManager = new RestRequestManagerImpl();
        }
        if (this.requestManager == null) {
            throw new ConfigurationException(connectionType.toString(), "Somethings wrong with ConnectionType, expected REST");
        }
        this.requestManager.setManageType(manageType);
        this.requestManager.setManageAllAtOnce(forceHeating);
        this.requestManager.setMaxManagedRequests(maximumAllowedRequests);
        this.isAutorun = autoRun;
    }


    @Override
    public void start() {
        if (this.isAutorun == false) {
            this.start = true;
            managerLogic();
        }
    }

    /**
     * Manages Request by ManageType (e.g. FIFO) and max Requests at once.
     */
    private void managerLogic() {
        this.requestManager.manageRequests(this.allRequests);
    }

    @Override
    public void stop() {
        if (this.isAutorun == false) {
            this.start = false;
            this.requestManager.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return this.isAutorun || this.start;
    }

    @Override
    public ConnectionType connectionType() {
        return this.connectionType;
    }

    @Override
    public void setAutoRun(boolean autoRun) {
        this.isAutorun = autoRun;
    }

    /**
     * Executes Logic of LeafletCommunicationController => Handle Requests by Manager.
     */
    @Override
    public void executeLogic() {
        if (this.isRunning()) {
            managerLogic();
        }
    }

    /**
     * Checks if there is something's somethings wrong with the connection / RemoteDevice.
     *
     * @return connectionOk boolean.
     */
    @Override
    public boolean communicationAvailable() {
        AtomicReference<Boolean> connectionAvailable = new AtomicReference<>(true);

        this.allRequests.forEach((key, value) -> {
            value.forEach(request -> {
                //Connection check only necessary if connection was ok before.
                if (connectionAvailable.get()) {
                    if (request.getRequest().connectionOk() == false || request.getCallbackRequest().connectionOk() == false) {
                        connectionAvailable.set(false);
                    }
                }
            });
        });
        return connectionAvailable.get();
    }

    @Override
    public ConnectionType getConnectionType() {
        return this.connectionType;
    }

    @Override
    public Map<Integer, List<RestRequest>> getAllRequests() {
        return this.allRequests;
    }

    @Override
    public RequestManager getRequestManager() {
        return this.requestManager;
    }

    @Override
    public void setMaxWaittime(int maxWaittime) {
        this.requestManager.setMaxWaittime(maxWaittime);
    }

    @Override
    public RestRequestManager getRestManager() {
        return this.requestManager;
    }

    /**
     * Adds RestRequests to allRequests. This is important for the Manager handling the Requests.
     *
     * @param additionalRequests the additional Requests.
     */
    @Override
    public void addRestRequests(Map<Integer, List<RestRequest>> additionalRequests) {
        additionalRequests.keySet().forEach(key -> {
            if (this.allRequests.containsKey(key)) {
                additionalRequests.get(key).forEach(request -> {
                    if (!this.allRequests.get(key).contains(request)) {
                        this.allRequests.get(key).add(request);
                    }
                });
            } else {
                this.allRequests.put(key, additionalRequests.get(key));
            }
        });
    }
}
