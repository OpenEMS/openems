package io.openems.edge.temperature.sensor.temperaturesensor;

import io.openems.edge.common.channel.Channel;

public interface TemperatureSensor {

    Channel<Integer> getTemperatureOfSensor();

    String getTemperatureSensorId();
}
