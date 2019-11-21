package io.openems.edge.temperatureBoard.api;

import com.pi4j.wiringpi.Spi;
import io.openems.edge.temperatureBoard.api.pins.Pin;
import io.openems.edge.temperatureBoard.api.pins.PinImpl;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;


public abstract class AdcImpl implements Adc {
    private List<Pin> pins = new ArrayList<>();
    private int spiChannel;
    private int inputType;
    private byte maxSize;
    private String circuitBoardId;
    private boolean initialized = false;

    public AdcImpl(List<Long> pins, int inputType, byte maxSize) {
        int position = 0;
        for (long l : pins) {
            this.pins.add(new PinImpl(l, position++));
        }
        this.inputType = inputType;
        this.maxSize = maxSize;
    }

    @Override
    public void initialize(int spiChannel, int frequency, String circuitBoardId) {
        if (!initialized) {
            this.circuitBoardId = circuitBoardId;
            this.spiChannel = spiChannel;
            this.initialized = true;

            Spi.wiringPiSPISetup(spiChannel, frequency);
        }
    }

    @Override
    public List<Pin> getPins() {
        return pins;
    }

    @Override
    public int getSpiChannel() {
        if (!initialized) {
            return -1;
        }
        return spiChannel;
    }

    @Override
    public int getInputType() {
        return inputType;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public byte getMaxSize() {
        return maxSize;
    }

    @Override
    public String getCircuitBoardId() {
        if (!initialized) {
            return "not initialized yet";
        }
        return circuitBoardId;
    }

    @Override
    public void deactivate(){
        //TODO actually nothing to do, for Temperature purposes, but maybe in future
    }
}
