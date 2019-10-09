package io.openems.edge.raspberrypi.circuitboard.api.adc.pins;

public class Pin {

    private final long value;
    private final int position;
    private boolean isUsed = false; //Important for Sensor
    //maybe father Sensor is needed, i'll look into that with further impl.
    //private String fatherSensor;
    private String usedBy; //Important for SensorType

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

    //important for SensorType
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