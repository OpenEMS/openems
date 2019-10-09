package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensors.Sensor;
import io.openems.edge.raspberrypi.sensors.task.TemperatureDigitalReadTask;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import javax.naming.ConfigurationException;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Temperature Sensor", immediate = true,
configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSensor extends Sensor implements OpenemsComponent, TemperatureSensoric {
@Reference
    SpiInitial spiInitial;
    private final String sensorType = "Temperature";


        protected TemperatureSensor(Config config, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
            super(config.sensorId(),"Temperature",config.circuitBoardId(),config.adcNumber(),config.pinPosition(), firstInitialChannelIds,furtherInitialChannelIds);

        }

    @Activate
    public void activate() throws ConfigurationException {
        super.addToCircuitBoard();

        spiInitial.addTask(this.getId(), new TemperatureDigitalReadTask(getTemperature(), this.getId(), super.getVersionId(),
                spiInitial.getAdcList().get(getIndexAdcOfCircuitBoard()), getPinPosition()));

    }

@Deactivate
    public void deactivate() {

}



}
