package io.openems.edge.bridge.spi.api;


import io.openems.edge.bridge.spi.task.Task;

public interface BridgeSpi {

    public void addTask(String sourceId, Task task);

    public void removeTask(String sourceId);

}