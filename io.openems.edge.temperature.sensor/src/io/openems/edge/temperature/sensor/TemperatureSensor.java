package io.openems.edge.temperature.sensor;

import io.openems.edge.common.channel.Channel;

public interface TemperatureSensor {

    Channel<Integer> getTemperatureOfSensor();

    String getTemperatureSensorId();
}
