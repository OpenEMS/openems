package io.openems.edge.bridge.spi.task;


import org.osgi.service.cm.ConfigurationException;

public abstract class SpiTaskImpl implements SpiTask {
    private final int spiChannel;
    private final String parentCircuitBoard;

    public SpiTaskImpl(int spiChannel, String parentCircuitBoard) {
        this.spiChannel = spiChannel;
        this.parentCircuitBoard = parentCircuitBoard;
    }

    public abstract byte[] getRequest();

    public abstract void setResponse(byte[] data);

    @Override
    public int getSpiChannel() {
        return spiChannel;
    }

    @Override
    public String getParentCircuitBoard() {
        return parentCircuitBoard;
    }
}


