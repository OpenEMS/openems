package io.openems.edge.raspberrypi.spi;

import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.subchannel.SpiSensor;

import java.util.List;
import java.util.Map;

public interface SpiInitial {
    void addSpiList(SpiSensor spiSensor, SensorType sensorType);

    boolean addAdcList(Adc adc);
    List<Adc> getAdcList();

    Map<SensorType, Adc> getAdcPart();

    boolean addAdcPart(SensorType sensorType, Adc adc);
}
