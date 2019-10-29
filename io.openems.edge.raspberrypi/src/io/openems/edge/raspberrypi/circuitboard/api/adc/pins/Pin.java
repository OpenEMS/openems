package io.openems.edge.raspberrypi.circuitboard.api.adc.pins;

import java.util.Objects;

public class Pin {

    private final long value;
    private final int position;
    private boolean isUsed = false;
    private String usedBy;

    public Pin(long value, int position) {
        this.value = value;
        this.position = position;
    }

    public int getPosition() {
        return this.position;
    }

    public long getValue() {
        return this.value;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public String getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(String usedBy) {
        if (!isUsed) {
            this.usedBy = usedBy;
            setUsed(true);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pin pin = (Pin) o;
        return position == pin.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }
}