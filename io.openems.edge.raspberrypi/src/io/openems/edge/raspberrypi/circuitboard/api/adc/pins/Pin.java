package io.openems.edge.raspberrypi.circuitboard.api.adc.pins;

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

}