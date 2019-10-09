package io.openems.edge.raspberrypi.circuitboard.api.adc.mcpModels.type8;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;

import java.util.List;

public abstract class Type8 extends Adc {

    private boolean initialzed = false;

    protected Type8(List<Long> pins, int inputType)
    {super(pins,inputType,(byte)8);
    }
}



