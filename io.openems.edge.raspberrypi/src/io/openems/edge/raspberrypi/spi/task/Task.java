package io.openems.edge.raspberrypi.spi.task;

import javax.naming.ConfigurationException;

public abstract class Task {
    private final int spiChannel;
    private final String parentCircuitBoard;

    public Task(int spiChannel, String parentCircuitBoard) {
    this.spiChannel = spiChannel;
    this.parentCircuitBoard = parentCircuitBoard;
    }

    public abstract byte[] getRequest() throws ConfigurationException;

    public abstract void setResponse(byte[] data);

    public int getSpiChannel() {
        return spiChannel;
    }

    public String getParentCircuitBoard() {
        return parentCircuitBoard;
    }
}


