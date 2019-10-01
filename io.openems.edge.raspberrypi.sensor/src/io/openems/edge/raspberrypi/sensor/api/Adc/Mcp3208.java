package io.openems.edge.raspberrypi.sensor.api.Adc;

import com.pi4j.io.gpio.Pin;
import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.PinList;
import io.openems.edge.raspberrypi.sensor.api.Board;

public class Mcp3208 extends Typ8 {
    protected Mcp3208(Board board, int id) {
        super(PinList.Mcp_3208.getPinList(), board, id);
    }
}
