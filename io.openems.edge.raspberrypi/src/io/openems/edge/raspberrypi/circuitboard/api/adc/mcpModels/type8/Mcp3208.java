package io.openems.edge.raspberrypi.circuitboard.api.adc.mcpModels.type8;

import io.openems.edge.raspberrypi.circuitboard.api.adc.pins.PinList;


public class Mcp3208 extends Type8 {
    public Mcp3208() {
        super(PinList.Mcp_3208.getPinList(), PinList.Mcp_3208.getInputType());
    }
}
