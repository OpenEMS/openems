package io.openems.edge.bridgei2c;

import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.relaisBoard.RelaisBoardImpl;

import java.util.List;

public interface I2cBridge {

    void addTask(String id, I2cTask task);

    void removeTask(String id);

    void addRelaisBoard(RelaisBoardImpl relaisBoard);

    List<RelaisBoardImpl> getRelaisBoardList();
}
