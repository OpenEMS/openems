package io.openems.edge.thermometer.virtual;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerVirtual;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Device.Thermometer.Virtual",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class ThermometerVirtualImpl extends AbstractOpenemsComponent implements OpenemsComponent, ThermometerVirtual, Thermometer, EventHandler {

    public ThermometerVirtualImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerVirtual.ChannelId.values(),
                Thermometer.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.getTemperature().setNextValue(Integer.MIN_VALUE);
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            //TODO WORK WITH CYCLES
            Optional<Integer> currentTemp = this.getVirtualTemperature();
            currentTemp.ifPresent(integer -> this.getTemperature().setNextValue(integer));
        }
    }

    @Override
    public String debugLog() {
        Optional<Integer> currentTemp = Optional.ofNullable(this.getTemperature().value().isDefined() ? this.getTemperature().value().get() : null);
        AtomicReference<String> returnString = new AtomicReference<>("NotDefined");
        currentTemp.ifPresent(integer -> {
            if (integer != Integer.MIN_VALUE) {
                returnString.set(integer.toString());
            }
        });
        return returnString.get() + " dC" + "\n";
    }
}
