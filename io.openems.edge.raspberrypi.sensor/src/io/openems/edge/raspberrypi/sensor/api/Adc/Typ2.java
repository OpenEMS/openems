package io.openems.edge.raspberrypi.sensor.api.Adc;

import io.openems.edge.raspberrypi.sensor.api.Board;

import java.util.List;

public abstract class Typ2 extends Adc {

    public Typ2(List<Long> pins,int inputType, Board board, int id, int SpiChannel){
        super(pins, inputType ,(byte)2, board, id, SpiChannel );
    }
}
