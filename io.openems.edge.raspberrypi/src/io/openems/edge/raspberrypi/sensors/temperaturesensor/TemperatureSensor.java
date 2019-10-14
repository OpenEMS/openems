package io.openems.edge.raspberrypi.sensors.temperaturesensor;


import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin;
import io.openems.edge.raspberrypi.sensors.Sensor;
import io.openems.edge.raspberrypi.sensors.task.TemperatureDigitalReadTask;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Temperature Sensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSensor extends Sensor implements OpenemsComponent, TemperatureSensoric {
    @Reference
    private SpiInitial spiInitial;
    private final String sensorType = "Temperature";
    private Adc adcForTemperature;
    private final Logger log = LoggerFactory.getLogger(TemperatureSensor.class);
    protected TemperatureSensor(Config config, ComponentContext context) {
        super(config.sensorId(), "Temperature", config.circuitBoardId(), config.spiChannel(),
                config.pinPosition(), config.spiInitial_id(), config.enabled(),
                TemperatureSensoric.ChannelId.values());
    }

    @Activate
    public void activate() throws ConfigurationException {
        super.activate(getComponentContext());
        for (CircuitBoard fromConsolinno : spiInitial.getCircuitBoards()) {
            if (fromConsolinno.getCircuitBoardId().equals(this.getCircuitBoardId())) {
                for (Adc adc : fromConsolinno.getAdcList()
                ) {
                    if (adc.getSpiChannel() == getSpiChannel()) {
                        adcForTemperature = adc;
                        if (adc.getPins().get(getPinPosition()) != null) {
                            Pin wantToUse = adc.getPins().get(getPinPosition());
                            if (wantToUse.isUsed() && !wantToUse.getUsedBy().equals(getId())) {
                                throw new ConfigurationException(
                                        "Pin is already used", "Pin is already used by "
                                        + wantToUse.getUsedBy());
                            } else {
                                spiInitial.addTask(this.getId(), new TemperatureDigitalReadTask(getTemperature(),
                                        super.getVersionId(), adcForTemperature, getPinPosition()));
                                wantToUse.setUsed(true);
                                wantToUse.setUsedBy(getId());
                                return;
                            }
                        } else {
                            throw new ConfigurationException("Wrong Pin",
                                    "The PinPosition" + getPinPosition() + "couldn't be found on the Adc");
                        }
                    } else {
                        throw new ConfigurationException("Wrong SpiChannel", "SpiChannel was wrong");
                    }
                }
            } else {
                throw new ConfigurationException("Wrong CircuitBoard ID", "CircuitBoard id was wrong");
            }
        }
    }

    @Deactivate
    public void deactivate() {
        spiInitial.removeTask(this.getId());
        adcForTemperature.getPins().get(getPinPosition()).setUsed(false);
    }

    @Override
    public String debugLog() {
        return "T:" + this.getTemperature().value().asString();
    }

}
