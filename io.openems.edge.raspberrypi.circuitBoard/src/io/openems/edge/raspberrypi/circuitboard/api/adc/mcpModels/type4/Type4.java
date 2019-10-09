package io.openems.edge.raspberrypi.circuitboard.api.adc.mcpModels.type4;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;

import java.util.List;

public abstract class Type4 extends Adc {

    public Type4(List<Long> pins, int inputType, int id, int spiChannel, String circuitBoardId) {
        super(pins, inputType, (byte)4, id, spiChannel, circuitBoardId);
    }
}
