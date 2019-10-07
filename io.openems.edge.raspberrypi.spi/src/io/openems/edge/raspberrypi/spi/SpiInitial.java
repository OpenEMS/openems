package io.openems.edge.raspberrypi.spi;

import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;


import java.util.List;
import java.util.Map;

public interface SpiInitial {

    boolean addAdcList(Adc adc);
    List<Adc> getAdcList();
    List<Sensor> getSensorList();
    List<SensorType> getSensorTypeList();

    Map <String, List<String>> getSensorManager();

    Map<String, Map<Integer, List<Integer>>> getAdcManager();

    Map<Integer, String> getSpiManager();
    boolean addToSensorManager(String child, String father);
    List<Integer> getFreeSpiChannels();



}
