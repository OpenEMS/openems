package io.openems.edge.raspberrypi.sensor.api.Adc.Pins;

public class Pin {

    private final long value;
    private final int position;
    private boolean isUsed; //Important for Sensor
    private String fatherSensor;
    private String usedBy; //Important for SensorType
    public Pin(long value, int position) {
        this.value = value;
        this.position=position;

    }

    public int getPosition(){
        return this.position;
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
        this.usedBy = usedBy;
    }

    public void setFatherSensor(String fatherSensor) {
        this.fatherSensor = fatherSensor;
    }

    public String getFatherSensor() {
        return fatherSensor;
    }
}
