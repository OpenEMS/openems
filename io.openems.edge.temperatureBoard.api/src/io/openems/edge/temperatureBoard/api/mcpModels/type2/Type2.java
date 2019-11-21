package io.openems.edge.temperatureBoard.api.mcpModels.type2;

import io.openems.edge.temperatureBoard.api.AdcImpl;

import java.util.List;

public abstract class Type2 extends AdcImpl {
    public Type2(List<Long> pins, int inputType, int id, int spiChannel, String circuitBoardId) {
        super(pins, inputType, (byte) 2);
    }
}
