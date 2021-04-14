package io.openems.edge.controller.heatnetwork.communication.api;

public interface RequestManager {

    void setMaxManagedRequests(int requestNo);

    int getMaxRequestsAtOnce();

    void setManageAllAtOnce(boolean manageAllAtOnce);

    void setManageType(ManageType type);

    ManageType getManageType();

    void stop();

    void setMaxWaittime(int maxWaittime);
}
