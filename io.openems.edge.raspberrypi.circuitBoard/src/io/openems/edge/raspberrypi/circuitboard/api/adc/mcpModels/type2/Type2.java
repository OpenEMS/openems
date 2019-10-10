package io.openems.edge.raspberrypi.circuitboard.api.adc.mcpModels.type2;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;

import java.util.List;

public abstract class Type2 extends Adc {
    public Type2(List<Long> pins, int inputType, int id, int spiChannel, String circuitBoardId) {
        super(pins, inputType, (byte) 12);
    }
}
