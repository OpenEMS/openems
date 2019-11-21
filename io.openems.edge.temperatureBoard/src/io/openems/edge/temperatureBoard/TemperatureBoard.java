package io.openems.edge.temperatureBoard;

import io.openems.edge.temperatureBoard.api.Adc;

import java.util.List;

public interface TemperatureBoard {
    short getMaxCapacity();
    String getCircuitBoardId();
    String getVersionId();
    List<Adc> getAdcList();
}
