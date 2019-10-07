package io.openems.edge.raspberrypi.sensor.sensortype.Temperature;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensor.Sensor;
import io.openems.edge.raspberrypi.sensor.sensortype.SensorType;
import io.openems.edge.raspberrypi.sensor.sensortype.digitalReadTask.DigitalReadTaskOnePin;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd= Config.class, factory=true)
@Component(name="TemperatureSensor",
immediate = true,
configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSensor extends SensorType implements TemperatureSensoric, OpenemsComponent {

    @Reference
    protected SpiInitialImpl spiInitial;

    public TemperatureSensor(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                             io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    @Activate
    void activate(Config config) {


            spiInitial.addTask(this.id(), new DigitalReadTaskOnePin(getTemperature(),
                    this.id(), Integer.parseInt(config.adcId()),
                    Integer.parseInt(config.pinPositions())));
            //TODO Change in future --> Only one adc and one Pin Pos per concrete Sensor allowed atm (Father sensor is the same)

    }

}
