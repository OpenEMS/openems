package io.openems.edge.raspberrypi.spi;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;
import java.util.List;


public interface SpiInitial {

    List<CircuitBoard> getCircuitBoards();

    void addTask(String sourceId, Task task);

    void removeTask(String sourceId);

}
