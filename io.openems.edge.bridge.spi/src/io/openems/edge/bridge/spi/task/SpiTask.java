package io.openems.edge.bridge.spi.task;

import org.osgi.service.cm.ConfigurationException;

public interface SpiTask {

    byte[] getRequest();

    void setResponse(byte[] data);

    int getSpiChannel();

    String getParentCircuitBoard();
    String getTemperatureSensorId();

}
