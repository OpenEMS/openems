package io.openems.edge.temperature.board.api.mcpmodels.type8;

import io.openems.edge.temperature.board.api.AbstractAdc;

import java.util.List;

abstract class Type8 extends AbstractAdc {

    Type8(List<Long> pins, int inputType) {
        super(pins, inputType, (byte) 8);
    }
}



