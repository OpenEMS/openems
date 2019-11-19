package io.openems.edge.bridgei2c;


import com.pi4j.io.gpio.GpioProviderBase;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.relaisboardmcp.Mcp;

import java.util.List;

public interface I2cBridge {

    void addMcp(Mcp mcp);
    List<Mcp> getMcpList();
    void removeMcp(Mcp mcp);
    void addGpioDevice(String id, GpioProviderBase gpio);
    void removeGpioDevice(String id);
    void addI2cTask(String id, I2cTask i2cTask) throws OpenemsException;
    void removeI2cTask(String id);


}
