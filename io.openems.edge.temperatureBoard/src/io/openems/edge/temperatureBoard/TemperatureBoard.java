package io.openems.edge.temperatureBoard;

import io.openems.edge.temperatureBoard.api.Adc;

import java.util.Set;

public interface TemperatureBoard {
    short getMaxCapacity();
    String getCircuitBoardId();
    String getVersionId();
    Set<Adc> getAdcList();
}
