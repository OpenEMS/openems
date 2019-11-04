package io.openems.edge.bridgei2c.task;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;

import javax.naming.ConfigurationException;

public abstract class I2cTask {
    private String relaisBoard;

    public I2cTask(String relaisBoard) {
        this.relaisBoard = relaisBoard;
    }

    public String getRelaisBoard() {
        return this.relaisBoard;
    }

    public abstract Channel<Boolean> getReadChannel();

    public abstract WriteChannel<Boolean> getWriteChannel();

    public abstract void activate();

    public abstract void deactivate();

    public abstract boolean isActive();

    public abstract boolean isReverse();

    public abstract int getPosition();
}
