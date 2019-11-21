package io.openems.edge.temperatureBoard.api.pins;

public interface Pin {
    int getPosition();

    long getValue();

    boolean isUsed();

    String getUsedBy();

    void setUsedBy(String usedBy);

    void setUnused();
}
