package io.openems.edge.bridgei2c;


import com.pi4j.io.gpio.GpioProviderBase;
import io.openems.edge.relaisboardmcp.Mcp;

import java.util.List;

public interface I2cBridge {

    void addMcp(Mcp mcp);
    List<Mcp> getMcpList();
    void removeMcp(Mcp mcp);
    void addGpioDevice(String id, GpioProviderBase gpio);
    void removeGpioDevice(String id, GpioProviderBase gpio);

}
