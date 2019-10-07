package io.openems.edge.raspberrypi.spi.task;

import javax.naming.ConfigurationException;

public abstract class Task {
    public Task() {

    }
    public abstract byte[] getRequest() throws ConfigurationException;

    public abstract void setResponse(byte[] data);

}


