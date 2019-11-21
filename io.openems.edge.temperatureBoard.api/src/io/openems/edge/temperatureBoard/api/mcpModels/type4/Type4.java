package io.openems.edge.temperatureBoard.api.mcpModels.type4;


import io.openems.edge.temperatureBoard.api.AdcImpl;

import java.util.List;

public abstract class Type4 extends AdcImpl {

    public Type4(List<Long> pins, int inputType, int id, int spiChannel, String circuitBoardId) {
        super(pins, inputType, (byte)4);
    }
}
