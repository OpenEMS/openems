package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensors.Sensor;
import io.openems.edge.raspberrypi.sensors.task.TemperatureDigitalReadTask;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Temperature Sensor", immediate = true,
configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSensor extends Sensor implements OpenemsComponent, TemperatureSensoric {
@Reference
    SpiInitial spiInitial;
    private final String sensorType = "Temperature";


        protected TemperatureSensor(Config config, ComponentContext context) {
            super(config.sensorId(),"Temperature", config.circuitBoardId(), config.adcNumber(),config.pinPosition(), config.spi_id(),config.enabled(),TemperatureSensoric.ChannelId.values());
        }



    @Activate
    public void activate(Config config, ComponentContext context) throws ConfigurationException {

        super.addToCircuitBoard();

        spiInitial.addTask(this.getId(), new TemperatureDigitalReadTask(getTemperature(), super.getVersionId(),
                spiInitial.getAdcList().get(getIndexAdcOfCircuitBoard()), getPinPosition()));

    }

    @Override
    public String debugLog() {
        return "T:" + this.getTemperature().value().asString();
    }

}
