package io.openems.edge.raspberrypi.sensors.temperaturesensor;


import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.circuitboard.CircuitBoard;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.adc.pins.Pin;
import io.openems.edge.raspberrypi.sensors.task.TemperatureDigitalReadTask;
import io.openems.edge.raspberrypi.spi.SpiInitial;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Designate(ocd = Config.class, factory = true)
@Component(name = "TemperatureSensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class TemperatureSensorImpl extends AbstractOpenemsComponent implements OpenemsComponent, TemperatureSensoric, TemperatureSensor {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private SpiInitial spiInitial;

    private String id;
    private String circuitBoardId;
    private String versionId;
    private int spiChannel;
    private int pinPosition;
    private String servicePid;
    private String alias;
    //private final String sensorType = "Temperature";
    private Adc adcForTemperature;
    private final Logger log = LoggerFactory.getLogger(TemperatureSensorImpl.class);


    public TemperatureSensorImpl() {
        super(OpenemsComponent.ChannelId.values(), TemperatureSensoric.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.id = config.id();
        this.circuitBoardId = config.circuitBoardId();
        this.spiChannel = config.spiChannel();
        this.pinPosition = config.pinPosition();
        this.alias = config.alias();

        Optional<CircuitBoard> optCb = spiInitial.getCircuitBoards().stream().filter(
                fromConsolinno -> fromConsolinno.getCircuitBoardId().equals(this.circuitBoardId)).findFirst();
        if (optCb.isPresent()) {
            this.versionId = optCb.get().getVersionId();
            Optional<Adc> optAdc = optCb.get().getAdcList().stream().filter(adc -> adc.getSpiChannel() == this.spiChannel).findFirst();
            if (optAdc.isPresent()) {
                this.adcForTemperature = optAdc.get();
                Optional<Pin> opt = adcForTemperature.getPins().stream().filter(pin -> pin.getPosition() == this.pinPosition).findFirst();
                if (opt.isPresent()) {
                    Pin wantToUse = opt.get();
                    if (wantToUse.isUsed() && !wantToUse.getUsedBy().equals(this.id)) {
                        throw new ConfigurationException(
                                "Pin is already used", "Pin is already used by "
                                + wantToUse.getUsedBy());
                    } else {
                        TemperatureDigitalReadTask task = new TemperatureDigitalReadTask(this.getTemperature(),
                                this.versionId, adcForTemperature, this.pinPosition, this.circuitBoardId);
                        spiInitial.addTask(this.id, task);
                        wantToUse.setUsedBy(this.id);
                        return;
                    }
                } else {
                    throw new ConfigurationException("Wrong Pin",
                            "The PinPosition" + this.pinPosition + "couldn't be found on the Adc");

                }
            } else {
                throw new ConfigurationException("Wrong SpiChannel", "SpiChannel was wrong");
            }
        } else {
            throw new ConfigurationException("Wrong CircuitBoard ID", "CircuitBoard id was wrong");
        }
    }



    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
        spiInitial.removeTask(this.id);
        adcForTemperature.getPins().get(this.pinPosition).setUsed(false);
        adcForTemperature = null;
    }

    @Override
    public String debugLog() {
        if (spiInitial.checkIfBoardIsPresent(this.circuitBoardId)) {
            return "T:" + this.getTemperature().value().asString() + " of TemperatureSensor: " + this.id + this.alias;
        } else {
            return null;
        }
    }

    @Override
    public String getTemperatureSensorId() {
        return this.id;
    }

    @Override
    public Channel<Integer> getTemperatureOfSensor() {
        return this.getTemperature();
    }


}
