package io.openems.edge.raspberrypi.spi;

import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;

import java.util.List;


public interface SpiInitial {

    List<CircuitBoard> getCircuitBoards();

    boolean checkIfBoardIsPresent(String circuitBoardId);

    void addCircuitBoards(CircuitBoard cb);

    void addTask(String sourceId, Task task);

    void removeTask(String sourceId);

    void removeCircuitBoard(CircuitBoard circuitBoard);
}
