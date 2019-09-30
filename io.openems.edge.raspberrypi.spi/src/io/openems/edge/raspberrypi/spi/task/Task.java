package io.openems.edge.raspberrypi.spi.task;

import com.pi4j.io.spi.SpiChannel;

public abstract class Task {
//TODO!!!! OVERWRITE SPICHANNEL!!! Expand Libs!
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

}
