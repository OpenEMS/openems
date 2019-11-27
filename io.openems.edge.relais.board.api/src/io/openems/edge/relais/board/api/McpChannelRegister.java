package io.openems.edge.relais.board.api;

import io.openems.edge.relais.board.api.task.McpTask;

import java.util.Map;

public interface McpChannelRegister {
    void setPosition(int position, boolean activate);

    void shift();

    void addToDefault(int position, boolean activate);

    void addTask(String id, McpTask mcpTask);

    void removeTask(String id);

    String getParentCircuitBoard();

    void deactivate();

    Map<Integer, Boolean> getValuesPerDefault();

}
