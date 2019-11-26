package io.openems.edge.temperature.board.api.pins;

public interface Pin {
    int getPosition();

    long getValue();

    boolean isUsed();

    String getUsedBy();

    boolean setUsedBy(String usedBy);

    void setUnused();
}
