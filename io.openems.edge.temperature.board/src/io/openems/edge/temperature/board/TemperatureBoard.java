package io.openems.edge.temperature.board;

import io.openems.edge.temperature.board.api.Adc;

import java.util.Set;

public interface TemperatureBoard {
    short getMaxCapacity();

    String getCircuitBoardId();

    String getVersionId();

    Set<Adc> getAdcList();
}
