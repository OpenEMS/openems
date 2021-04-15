package io.openems.edge.heater.gasboiler.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.heater.gasboiler.viessmann.api.GasBoiler;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Designate(ocd = ConfigOneRelay.class, factory = true)
@Component(name = "GasBoilerOneRelay",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class GasBoilerOneRelayImpl extends AbstractOpenemsComponent implements OpenemsComponent, GasBoiler, Heater, EventHandler {

    private final Logger log = LoggerFactory.getLogger(GasBoilerOneRelayImpl.class);

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    private Relay relay;
    private int thermalOutput;
    private boolean isEnabled;
    private int cycleCounter = 0;

    ConfigOneRelay config;


    public GasBoilerOneRelayImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, ConfigOneRelay config) throws OpenemsError.OpenemsNamedException {

        super.activate(context, config.id(), config.alias(), config.enabled());

        this.config = config;

        if (this.cpm.getComponent(config.relayId()) instanceof Relay) {
            this.relay = this.cpm.getComponent(config.relayId());
        }
        this.thermalOutput = config.maxThermicalOutput();
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        try {
            if (this.relay != null) {
                this.relay.getRelaysWriteChannel().setNextWriteValue(false);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        if (this.relay != null && this.relay.isEnabled()) {
            this.relay.getRelaysWriteChannel().setNextWriteValue(true);
            return this.thermalOutput;
        } else {
            try {
                if (cpm.getComponent(config.relayId()) instanceof Relay) {
                    this.relay = cpm.getComponent(config.relayId());
                    this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                    return this.thermalOutput;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't find component!" + e.getMessage());
                return 0;


            }

        }
        return 0;
    }


    @Override
    public int getMaximumThermalOutput() {
        return this.thermalOutput;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        if (this.relay != null && this.isEnabled) {
            this.relay.getRelaysWriteChannel().setNextWriteValue(false);
            this.setState(HeaterState.OFFLINE.name());
        }
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public void requestMaximumPower() {
        if (isEnabled && this.relay != null) {
            try {
                this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                this.setState(HeaterState.RUNNING.name());
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel " + e.getMessage());
            }
        }
    }

    @Override
    public void setIdle() {
        if (isEnabled && this.relay != null) {
            try {
                this.relay.getRelaysWriteChannel().setNextWriteValue(false);
                this.setState(HeaterState.AWAIT.name());
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel " + e.getMessage());
            }
        }
    }

    @Override
    public String debugLog() {

        if (this.relay.getRelaysWriteChannel().value().isDefined()) {
            String active = this.relay.getRelaysWriteChannel().value().get() ? "active" : "not Active";
            return this.id() + " Status: " + active;
        }

        return "No Value available yet";

    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            channelmapping();
        }
    }

    protected void channelmapping() {
        // Decide state of enabledSignal.
        // The method isEnabledSignal() does get and reset. Calling it will clear the value (for that cycle). So you
        // need to store the value in a local variable.
        Optional<Boolean> enabledSignal = isEnabledSignal();
        if (enabledSignal.isPresent()) {
            isEnabled = enabledSignal.get();
            cycleCounter = 0;
        } else {
            // No value in the Optional.
            // Wait 5 cycles. If isEnabledSignal() has not been filled with a value again, switch to false.
            if (isEnabled) {
                cycleCounter++;
                if (cycleCounter > 5) {
                    isEnabled = false;
                }
            }
        }
    }
}
