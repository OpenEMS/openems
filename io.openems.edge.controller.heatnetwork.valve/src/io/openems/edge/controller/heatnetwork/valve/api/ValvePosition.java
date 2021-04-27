package io.openems.edge.controller.heatnetwork.valve.api;

public class ValvePosition {
    //key of Position, usually a Temperature
    int temperature;
    //value of Position, usually a ValvePosition
    double valvePosition;

    public ValvePosition(int temperature, double valvePosition) {
        this.temperature = temperature;
        this.valvePosition = valvePosition;
    }

    public int getTemperature() {
        return temperature;
    }

    public double getValvePosition() {
        return valvePosition;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public void setValvePosition(int valvePosition) {
        this.valvePosition = valvePosition;
    }
}
