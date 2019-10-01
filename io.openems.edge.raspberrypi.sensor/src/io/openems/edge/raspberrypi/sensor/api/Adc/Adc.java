package io.openems.edge.raspberrypi.sensor.api.Adc;

import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.Pin;
import io.openems.edge.raspberrypi.sensor.api.Board;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import jdk.nashorn.internal.ir.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

public abstract class Adc {
    @Reference
    protected SpiInitial spiInitial;
    //Adc abstract class: Created by Sensor Type
    private List<Pin> pins = new ArrayList<>();
    //TODO for later change to short
    private byte MAX_SIZE;
    private Board board;
    private int id;

    public Adc(List<Long> pins, byte max_size, Board board, int id) {
        this.MAX_SIZE = max_size;
        this.board = board;
        this.id = id;
        int position = 0;
        for (long l : pins) {

            this.pins.add(new Pin(l, position++));
        }
//Check if it should be extending AbstractOpenemsComponent
        //Just for UserCheck, if he did right job with Id
        this.spiInitial.addAdcList(this);
    }



    public Board getBoard() {
        return board;
    }

    public int getId() {
        return id;
    }

}
