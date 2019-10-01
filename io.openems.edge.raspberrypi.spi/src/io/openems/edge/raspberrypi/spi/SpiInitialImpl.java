package io.openems.edge.raspberrypi.spi;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.spi.api.BridgeSpi;
import io.openems.edge.raspberrypi.spi.subchannel.SpiSensor;
import io.openems.edge.raspberrypi.spi.task.Task;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Designate(ocd = Config.class, factory = true)
@Component(name = "SpiInitial")
@Config
public class SpiInitialImpl extends AbstractOpenemsComponent implements SpiInitial, BridgeSpi, EventHandler, OpenemsComponent {

    private Map<SpiSensor, SensorType> spiList = new HashMap<>();

    private List<Adc> adcList = new ArrayList<>();
    private Map<SensorType, Adc> adcPart = new HashMap<>();
    private String name;

    protected SpiInitialImpl(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    @Activate
    //TODO Create SPI List and ADC List, used by every device
    //TODO SPI Wiring Pi Setup; Do SPI Worker --> for every Channel
    //TODO handle Event
    public void activate(Config config) {
        this.name = config.id();
    }

    @Deactivate
    public void deactivate() {
        //TODO Close every SPI SubChannel and with it, it's connected Devices
        //TODO Worker Deactivate
        super.deactivate();
    }

    //TODO Write Tasks
    @Override
    public void addTask(String sourceId, Task task) {

    }

    @Override
    public void removeTask(String sourceId) {

    }

    @Override
    public void handleEvent(Event event) {

    }

    //useful for checks if SpiSensor-->Channel is already used or if adc already exists
    public Map<SpiSensor, SensorType> getSpiList() {
        return spiList;
    }

    public List<Adc> getAdcList() {
        return adcList;
    }

    @Override
    public void addSpiList(SpiSensor spiSensor, SensorType sensorType) {
        this.spiList.put(spiSensor, sensorType);

    }

    @Override
    public boolean addAdcList(Adc adc) {
        return this.adcList.add(adc);
    }

    @Override
    public Map<SensorType, Adc> getAdcPart() {
        return adcPart;
    }

    @Override
    public boolean addAdcPart(SensorType sensorType, Adc adc) {

        return this.adcPart.put(sensorType, adc) == null;
    }
}
