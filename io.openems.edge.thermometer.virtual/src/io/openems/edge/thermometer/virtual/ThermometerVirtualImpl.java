package io.openems.edge.thermometer.virtual;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerVirtual;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Virtual Thermometer represents a Thermometer and the temperature can be manipulated by other classes/Components.
 * If a WriteValue is defined, it will be set in the TemperatureChannel.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Virtual",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class ThermometerVirtualImpl extends AbstractOpenemsComponent implements OpenemsComponent, ThermometerVirtual, Thermometer, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ThermometerVirtualImpl.class);

    @Reference
    ComponentManager cpm;

    ChannelAddress refThermometer;
    boolean useAnotherChannelAsTemp;

    public ThermometerVirtualImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerVirtual.ChannelId.values(),
                Thermometer.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.getTemperatureChannel().setNextValue(Integer.MIN_VALUE);
        this.useAnotherChannelAsTemp = config.useAnotherChannelAsTemperature();
        if (this.useAnotherChannelAsTemp) {
            this.refThermometer = ChannelAddress.fromString(config.channelAddress());
        }

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            Optional<Integer> currentTemp = this.getVirtualTemperature();
            currentTemp.ifPresent(integer -> this.getTemperatureChannel().setNextValue(integer));
            if (currentTemp.isPresent() == false && this.useAnotherChannelAsTemp) {
                try {
                    Channel<?> temperature = this.cpm.getChannel(this.refThermometer);
                    if (temperature.value().isDefined()) {
                        this.getTemperatureChannel().setNextValue(temperature.value().get());
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find Channel: " + this.refThermometer.toString());
                }
            }

        }
    }

    @Override
    public String debugLog() {
        Optional<Integer> currentTemp = Optional.ofNullable(this.getTemperatureChannel().value().isDefined() ? this.getTemperatureChannel().value().get() : null);
        AtomicReference<String> returnString = new AtomicReference<>("Temperature not Defined for: " + super.id());
        currentTemp.ifPresent(integer -> {
            if (integer != Integer.MIN_VALUE) {
                returnString.set(integer.toString() + this.getTemperatureChannel().channelDoc().getUnit().getSymbol());
            }
        });
        return returnString.get() + "\n";
    }
}
