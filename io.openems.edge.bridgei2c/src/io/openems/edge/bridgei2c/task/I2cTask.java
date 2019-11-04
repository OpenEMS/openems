package io.openems.edge.bridgei2c.task;

import javax.naming.ConfigurationException;

public abstract class Task {
    private String relaisBoard;

    public Task(String relaisBoard){
        this.relaisBoard = relaisBoard;
    }

    public abstract byte[] getRequest() throws ConfigurationException;

    public abstract void setResponse(byte[] data);

    public abstract String getRelaisBoard();
}
