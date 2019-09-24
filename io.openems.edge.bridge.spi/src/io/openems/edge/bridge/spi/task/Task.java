package io.openems.edge.bridge.spi.task;

import com.pi4j.io.spi.SpiChannel;

public abstract class Task {

    private final SpiChannel spiChannel;

    public Task(SpiChannel spiChannel) {
        this.spiChannel = spiChannel;
    }

    public SpiChannel getChannel() {
        return spiChannel;
    }

    public abstract byte[] getRequest();

    public abstract void setResponse(byte[] data);

}