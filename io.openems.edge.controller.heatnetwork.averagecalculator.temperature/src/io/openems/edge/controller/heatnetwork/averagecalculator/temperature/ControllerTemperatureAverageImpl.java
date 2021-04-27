package io.openems.edge.controller.heatnetwork.averagecalculator.temperature;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.averagecalculator.temperature.api.ControllerTemperatureAverage;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.controller.temperature.averagecalculator")
public class ControllerTemperatureAverageImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ControllerTemperatureAverage {

    @Reference
    ComponentManager cpm;


    private final List<ChannelAddress> readThermometerChannel = new ArrayList<>();
    private ChannelAddress writeThermometer;
    AtomicInteger thermometerCountOfThisIteration = new AtomicInteger(0);
    AtomicInteger temperatureOfAllSensors = new AtomicInteger(0);

    public ControllerTemperatureAverageImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        List<String> readChannels = Arrays.asList(config.readThermometerChannel());
        OpenemsError.OpenemsNamedException[] err = {null};
        readChannels.forEach(entry -> {
            if (err[0] == null) {
                try {
                    readThermometerChannel.add(ChannelAddress.fromString(entry));
                } catch (OpenemsError.OpenemsNamedException e) {
                    err[0] = e;
                }
            }
        });
        try {
            this.writeThermometer = ChannelAddress.fromString(config.writeVirtualThermometerChannel());
        } catch (OpenemsError.OpenemsNamedException e) {
            err[0] = e;
        }
        if (err[0] != null) {
                throw new ConfigurationException(err[0].getMessage(), "Check your Config!");
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        this.temperatureOfAllSensors.set(0);
        this.thermometerCountOfThisIteration.set(0);

        this.readThermometerChannel.forEach(temp -> {
            try {
                if (cpm.getChannel(temp).value().isDefined()) {
                    Object temperature = cpm.getChannel(temp).value().get();
                    if (temperature instanceof Integer) {
                        this.temperatureOfAllSensors.getAndAdd((Integer) temperature);
                        this.thermometerCountOfThisIteration.getAndIncrement();
                    }
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        });
        if (thermometerCountOfThisIteration.get() > 0) {
            int temperature = this.temperatureOfAllSensors.get() / this.thermometerCountOfThisIteration.get();
            WriteChannel<Integer> writeThermometerChannel = this.cpm.getChannel(this.writeThermometer);
            writeThermometerChannel.setNextWriteValue(temperature);
        }
    }
}
