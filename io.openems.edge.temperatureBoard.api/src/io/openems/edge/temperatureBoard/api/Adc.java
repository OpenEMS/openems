package io.openems.edge.temperatureBoard.api;

import io.openems.edge.temperatureBoard.api.pins.Pin;

import java.util.List;

public interface Adc {
    void initialize(int spiChannel, int frequency, String circuitBoardId);

    List<Pin> getPins();

    int getSpiChannel();

    int getInputType();

    boolean isInitialized();

    byte getMaxSize();

    String getCircuitBoardId();

    void deactivate();
}
