package io.openems.edge.bridge.spi.task;

public interface SpiTask {

    byte[] getRequest();

    void setResponse(byte[] data);

    int getSpiChannel();

    String getParentCircuitBoard();

    String getTemperatureSensorId();

}
