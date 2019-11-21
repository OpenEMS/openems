package io.openems.edge.temperatureBoard.api.mcpModels.type8;

import io.openems.edge.temperatureBoard.api.pins.PinList;


public class Mcp3208 extends Type8 {
    public Mcp3208() {
        super(PinList.Mcp_3208.getPinList(), PinList.Mcp_3208.getInputType());
    }
}
