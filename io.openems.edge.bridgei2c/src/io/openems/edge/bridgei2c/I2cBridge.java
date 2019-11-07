package io.openems.edge.bridgei2c;


import io.openems.edge.relaisboardmcp.Mcp;

import java.util.List;

public interface I2cBridge {

    void addMcp(Mcp mcp);
    List<Mcp> getMcpList();
    void removeMcp(Mcp mcp);

}
