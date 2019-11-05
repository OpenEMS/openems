package io.openems.edge.bridgei2c;

import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.relaisboardmcp.Mcp;

import java.util.List;

public interface I2cBridge {

    void addTask(String id, I2cTask task);

    void removeTask(String id);

    void addMcp(Mcp mcp);

    List<Mcp> getMcpList();
}
