package io.openems.edge.relaisBoard;

import io.openems.edge.relaisboardmcp.task.McpTask;
import io.openems.edge.relaisboardmcp.Mcp;

public interface RelaisBoard {

    Mcp getMcpOfRelaisBoard();

    void addTask(String id, McpTask task);

    void removeTask(String id);

}
