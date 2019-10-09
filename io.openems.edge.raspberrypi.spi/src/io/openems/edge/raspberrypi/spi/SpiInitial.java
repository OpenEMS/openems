package io.openems.edge.raspberrypi.spi;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.circuitboard.*;
import io.openems.edge.raspberrypi.sensors.task.Task;

import java.util.List;
import java.util.Map;

public interface SpiInitial {

    //boolean addAdcList(Adc adc);
    //List<Adc> getAdcList();
    boolean addAdcList(Adc adc);
    List<Adc> getAdcList();
    List<Sensor> getSensorList();
    List<SensorType> getSensorTypeList();

    Map <String, List<String>> getSensorManager();

    Map<String, Map<Integer, List<Integer>>> getAdcManager();

    Map<Integer, Integer> getSpiManager();
    boolean addToSensorManager(String child, String father);
    List<Integer> getFreeSpiChannels();
    public List<Integer>getFreeAdcIds();
    public List<CircuitBoard> getCircuitBoards();

    public void addTask(String sourceId, Task task);


}
