package io.openems.edge.temperature.board.api.pins;

import java.util.Objects;

public class PinImpl implements Pin {

    private final long value;
    private final int position;
    private boolean isUsed = false;
    private String usedBy;

    public PinImpl(long value, int position) {
        this.value = value;
        this.position = position;
    }

    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public long getValue() {
        return this.value;
    }

    @Override
    public boolean isUsed() {
        return isUsed;
    }

    private void setUsed(boolean used) {
        isUsed = used;
    }

    @Override
    public String getUsedBy() {
        return usedBy;
    }

    @Override
    public boolean setUsedBy(String usedBy) {
        if (!isUsed) {
            this.usedBy = usedBy;
            setUsed(true);
            return true;
        }
        return false;
    }

    @Override
    public void setUnused() {
            setUsed(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PinImpl pin = (PinImpl) o;
        return position == pin.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }
}