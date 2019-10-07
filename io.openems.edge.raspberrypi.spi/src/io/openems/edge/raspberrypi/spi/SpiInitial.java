package io.openems.edge.raspberrypi.spi;

<<<<<<< HEAD
import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
=======
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.subchannel.SpiSensor;
>>>>>>> SPI

import java.util.List;
import java.util.Map;

<<<<<<< HEAD
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


=======
public interface SpiInitial {
    void addSpiList(SpiSensor spiSensor, SensorType sensorType);

    boolean addAdcList(Adc adc);
    List<Adc> getAdcList();

    Map<SensorType, Adc> getAdcPart();
>>>>>>> SPI

    boolean addAdcPart(SensorType sensorType, Adc adc);
}
