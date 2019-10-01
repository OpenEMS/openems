package io.openems.edge.raspberrypi.spi.api;

import io.openems.edge.raspberrypi.spi.task.Task;

public interface BridgeSpi {

    public void addTask(String sourceId, Task task);

    public void removeTask(String sourceId);

}
