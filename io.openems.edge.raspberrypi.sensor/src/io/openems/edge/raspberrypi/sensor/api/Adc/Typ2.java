package io.openems.edge.raspberrypi.sensor.api.Adc;

import io.openems.edge.raspberrypi.sensor.api.Board;

import java.util.List;

public abstract class Typ2 extends Adc {

    public Typ2(List<Long> pins, Board board, int id){
        super(pins, (byte)2, board, id);
    }
}
