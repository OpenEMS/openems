package io.openems.edge.raspberrypi.sensor.api.Adc;


import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.PinList;
import io.openems.edge.raspberrypi.sensor.api.Board;

public class Mcp3208 extends Typ8 {
<<<<<<< HEAD

    protected Mcp3208(Board board, int id, int spiChannel) {
        super(PinList.Mcp_3208.getPinList(), PinList.Mcp_3208.getInputType() ,board, id, spiChannel);
=======
    protected Mcp3208(Board board, int id) {
        super(PinList.Mcp_3208.getPinList(), board, id);
>>>>>>> SPI
    }
}
