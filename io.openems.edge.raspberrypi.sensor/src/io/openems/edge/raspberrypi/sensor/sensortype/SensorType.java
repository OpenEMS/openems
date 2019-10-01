package io.openems.edge.raspberrypi.sensor.sensortype;

import io.openems.edge.raspberrypi.sensor.Sensoric;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd= Config.class, factory=true)
@Component(name="SensorType")

public abstract class SensorType implements Sensoric {
    @Reference
    protected SpiInitialImpl spiInitial;
    //needs the adcParts or check if they can be allocated to them

    private String sensorId; //TODO Set via Config
    private String ChannelId; //Will be the Indicator for what it ll be used--> Like Temperature for temperature Channel
    //TODO Implement SpiSensor
    //private SpiSensor spiSensor;
    //TODO Via Config, get SensorTypeID; Father Sensor ID; Chip ID; PinID, SpiChID;
    public SensorType (){}
    //TODO @Activate --> Add SpiListe this and


}
