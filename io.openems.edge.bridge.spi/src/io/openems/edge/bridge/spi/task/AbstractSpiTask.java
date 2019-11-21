package io.openems.edge.bridge.spi.task;


public abstract class AbstractSpiTask implements SpiTask {
    private final int spiChannel;
    private final String parentCircuitBoard;

    public AbstractSpiTask(int spiChannel, String parentCircuitBoard) {
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