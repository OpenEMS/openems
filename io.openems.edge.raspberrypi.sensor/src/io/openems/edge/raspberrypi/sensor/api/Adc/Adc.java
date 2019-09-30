package io.openems.edge.raspberrypi.sensor.api.Adc;

import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.Pin;
import io.openems.edge.raspberrypi.sensor.api.Board;

import java.util.ArrayList;
import java.util.List;

public abstract class Adc {
//Adc abstract class: Created by Sensor Type
    private final List<Pin> pins=new ArrayList<>();
    //TODO for later change to short
    private final int MAX_SIZE;

    private Board board;
    private String id;

    protected Adc(List<Long> PinList, int max_size, Board board, String id) {
        MAX_SIZE = max_size;
        this.board=board;
        this.id=id;
        int position= 0;
        for (long l:PinList
             ) {

            pins.add(new Pin(l, position++));
        }

    }

    public Board getBoard() {
        return board;
    }

    public String getId(){return id;}
}
