package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import io.openems.edge.common.channel.Channel;

public interface TemperatureSensor {

    Channel<Integer> getTemperatureOfSensor();

    String getTemperatureSensorId();
}
