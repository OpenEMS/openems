package io.openems.edge.raspberrypi.spi;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;

import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.spi.task.Task;


import java.util.List;
import java.util.Map;

public interface SpiInitial {


    boolean addAdcList(Adc adc);

    List<Adc> getAdcList();

    Map<Integer, Integer> getSpiManager();

    List<Integer> getFreeSpiChannels();

    List<Integer> getFreeAdcIds();

    List<CircuitBoard> getCircuitBoards();

    void addTask(String sourceId, Task task);

    void removeTask(String sourceId);


}
