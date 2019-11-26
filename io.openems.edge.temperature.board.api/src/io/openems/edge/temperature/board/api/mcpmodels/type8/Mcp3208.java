package io.openems.edge.temperature.board.api.mcpmodels.type8;

import io.openems.edge.temperature.board.api.pins.PinList;


public class Mcp3208 extends Type8 {
    public Mcp3208() {
        super(PinList.Mcp_3208.getPinList(), PinList.Mcp_3208.getInputType());
    }

}
