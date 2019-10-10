package io.openems.edge.raspberrypi.circuitboard.api.adc;

import io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import jdk.nashorn.internal.ir.annotations.Reference;

import java.util.ArrayList;
import java.util.List;

public abstract class Adc {
    //TODO Add Input Type --> 12 bit or so for each concrete MCP
    //TODO IMPORTANT!!!! Adc needs SPI channel not the sensor!!!! remember!!
    @Reference
    private SpiInitial spiInitial;
    //Adc abstract class: Created by Sensor Type
    private List<io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin> pins = new ArrayList<>();
    private int spiChannel;
    private int inputType;
    private byte maxSize;
    private int id;
    private String circuitBoardId;
    private boolean initialized = false;

    public Adc(List<Long> pins, int inputType, byte maxSize) {
        int position = 0;

        for (long l : pins) {

            this.pins.add(new io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin(l, position++));
        }
        this.inputType = inputType;
        this.maxSize = maxSize;
    }

    public void initialize(int id, int spiChannel, String circuitBoardId) {
        if (!initialized) {

            this.circuitBoardId = circuitBoardId;
            this.id = id;
            this.spiChannel = spiChannel;
            this.initialized = true;
        }
    }

    public int getId() {
        if (!initialized) {
            return -1;
        }
        return id;
    }

    public List<Pin> getPins() {
        return pins;
    }

    public int getSpiChannel() {
        if (!initialized) {
            return -1;
        }
        return spiChannel;
    }

    public int getInputType() {
        return inputType;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public byte getMaxSize() {
        return maxSize;
    }

    public String getCircuitBoardId() {
        if (!initialized) {
            return "not initialized yet";
        }
        return circuitBoardId;
    }
}
