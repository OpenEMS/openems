package io.openems.edge.raspberrypi.sensor.sensortype;

import io.openems.edge.raspberrypi.sensor.api.Adc.AdcParts;
import io.openems.edge.raspberrypi.sensor.Sensoric;

public abstract class SensorType implements Sensoric {
    private String sensorId; //TODO Set via Config
    private AdcParts adc;
    private String ChannelId; //TODO Check if it's actually another Type like Doc or something
    //TODO Implement SpiSensor
    //private SpiSensor spiSensor;
    //TODO Via Config, get SensorTypeID; Father Sensor ID; Chip ID; PinID, SpiChID;
    public SensorType (){}
    //TODO @Activate --> Add SpiListe this and


}
