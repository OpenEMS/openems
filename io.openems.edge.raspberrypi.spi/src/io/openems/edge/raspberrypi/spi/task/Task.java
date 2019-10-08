package io.openems.edge.raspberrypi.spi.task;

import javax.naming.ConfigurationException;
//TODO atm only one channel possible -->Change to more (low priority)
public abstract class Task {
    private final int spiChannel;

    public Task(int spiChannel) {
    this.spiChannel=spiChannel;
    }

    public abstract byte[] getRequest() throws ConfigurationException;

    public abstract void setResponse(byte[] data);

    public int getSpiChannel() {
        return spiChannel;
    }
}


