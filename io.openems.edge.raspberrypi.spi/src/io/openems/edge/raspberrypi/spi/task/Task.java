package io.openems.edge.raspberrypi.spi.task;

public abstract class Task {
    public Task() {

    }
    public abstract byte[] getRequest();

    public abstract void setResponse(byte[] data);

}

}
