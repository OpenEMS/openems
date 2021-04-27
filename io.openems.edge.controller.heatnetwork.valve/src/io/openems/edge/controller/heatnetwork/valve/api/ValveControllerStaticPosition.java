package io.openems.edge.controller.heatnetwork.valve.api;

public interface ValveControllerStaticPosition extends ValveController {
    double getPositionByTemperature(int temperature);

    int getTemperatureByPosition(double position);

    void addPositionByTemperatureAndPosition(int temperature, int position);

}
