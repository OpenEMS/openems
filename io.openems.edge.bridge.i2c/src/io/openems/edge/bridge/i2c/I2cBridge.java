package io.openems.edge.bridge.i2c;

import io.openems.edge.bridge.i2c.task.I2cTask;
import io.openems.edge.pwm.module.api.IpcaGpioProvider;
import io.openems.edge.relais.board.api.Mcp;
import io.openems.common.exceptions.OpenemsException;

import java.util.List;

public interface I2cBridge {

    void addMcp(Mcp mcp);

    List<Mcp> getMcpList();

    void removeMcp(Mcp mcp);

    void addGpioDevice(String id, IpcaGpioProvider gpio);

    void removeGpioDevice(String id);

    void addI2cTask(String id, I2cTask i2cTask) throws OpenemsException;

    void removeI2cTask(String id);


}
