package io.openems.edge.relaisboardapi;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.relaisboardapi.task.McpTask;

public interface McpChannelRegister {
    void setPosition(int position, boolean activate);

    void shift() throws OpenemsError.OpenemsNamedException;
    void addToDefault(int position, boolean activate);

     void addTask(String id, McpTask mcpTask);

     void removeTask(String id);
     String getParentCircuitBoard();
     void deactivate();
}
