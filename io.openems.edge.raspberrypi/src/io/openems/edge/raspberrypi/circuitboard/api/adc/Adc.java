package io.openems.edge.raspberrypi.circuitboard.api.adc;

import io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin;
import com.pi4j.wiringpi.Spi;
import java.util.ArrayList;
import java.util.List;


public abstract class Adc {
    private List<Pin> pins = new ArrayList<>();
    private int spiChannel;
    private int inputType;
    private byte maxSize;
    private String circuitBoardId;
    private boolean initialized = false;

    public Adc(List<Long> pins, int inputType, byte maxSize) {
        int position = 0;
        for (long l : pins) {
            this.pins.add(new Pin(l, position++));
        }
        this.inputType = inputType;
        this.maxSize = maxSize;
    }

    public void initialize(int spiChannel, int frequency, String circuitBoardId) {
        if (!initialized) {
            this.circuitBoardId = circuitBoardId;
            this.spiChannel = spiChannel;
            this.initialized = true;

            //Spi.wiringPiSPISetup(spiChannel, frequency);
        }
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
