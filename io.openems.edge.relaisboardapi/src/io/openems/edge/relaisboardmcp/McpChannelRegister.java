package io.openems.edge.relaisboardmcp;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.relaisboardmcp.task.McpTask;

import java.util.List;

public interface McpChannelRegister {
    void setPosition(int position, boolean activate);

    void shift() throws OpenemsError.OpenemsNamedException;
    void addToDefault(int position, boolean activate);

     void addTask(String id, McpTask mcpTask);

     void removeTask(String id);
}
