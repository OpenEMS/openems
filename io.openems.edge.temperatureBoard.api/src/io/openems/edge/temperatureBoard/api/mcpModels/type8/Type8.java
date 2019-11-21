package io.openems.edge.temperatureBoard.api.mcpModels.type8;

import io.openems.edge.temperatureBoard.api.AbstractAdc;

import java.util.List;

abstract class Type8 extends AbstractAdc {

    Type8(List<Long> pins, int inputType) {
        super(pins, inputType, (byte) 8);
    }
}



