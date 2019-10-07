package io.openems.edge.raspberrypi.sensor.api.Adc;


import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.PinList;
import io.openems.edge.raspberrypi.sensor.api.Board;

public class Mcp3208 extends Typ8 {

    protected Mcp3208(Board board, int id, int spiChannel) {
        super(PinList.Mcp_3208.getPinList(), PinList.Mcp_3208.getInputType() ,board, id, spiChannel);
    }
}
