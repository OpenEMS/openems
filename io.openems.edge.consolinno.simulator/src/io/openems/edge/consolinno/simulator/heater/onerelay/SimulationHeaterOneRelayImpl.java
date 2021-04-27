package io.openems.edge.consolinno.simulator.heater.onerelay;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.Heater;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulation.Heater.OneRelay",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class SimulationHeaterOneRelayImpl extends AbstractOpenemsComponent implements OpenemsComponent, Heater, EventHandler {

    private static final int maxAllowedNullValues = 5;
    private int currentCycle = 0;
    private boolean lastKnownState = false;
    private WriteChannel<Boolean> writeChannel;


    @Reference
    ComponentManager cpm;

    public SimulationHeaterOneRelayImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        OpenemsComponent component = this.cpm.getComponent(config.relayId());


        ChannelAddress channelAddress = ChannelAddress.fromString(config.relayId() + "/WriteOnOff");
        Channel<?> testChannel = this.cpm.getChannel(channelAddress);
        if ((testChannel instanceof WriteChannel<?> && testChannel.getType() == OpenemsType.BOOLEAN) == false) {
            throw new ConfigurationException("ActivateMethod in SimulationHeater", "Component is not a Relay");
        } else {
            this.writeChannel = (WriteChannel<Boolean>) testChannel;
        }

    }

    @Deactivate
    public void deactivate() {
        try {
            this.writeChannel.setNextWriteValue(false);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
        super.deactivate();
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
        return 0;
    }

    @Override
    public int getMaximumThermalOutput() {
        return 0;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        this.writeChannel.setNextWriteValue(false);
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public void requestMaximumPower() {
        try {
            this.writeChannel.setNextWriteValue(true);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setIdle() {

    }

    @Override
    public void handleEvent(Event event) {
        if (EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE.equals(event.getTopic())) {
            Optional<Boolean> enabled = this.getEnableSignalChannel().getNextWriteValueAndReset();
            try {

                if (enabled.isPresent() && isEnabled()) {
                    this.writeChannel.setNextWriteValue(enabled.get());
                    currentCycle = 0;
                    lastKnownState = true;
                } else if (currentCycle < maxAllowedNullValues) {

                    this.writeChannel.setNextWriteValue(lastKnownState);
                    currentCycle++;

                } else {
                    this.writeChannel.setNextWriteValue(false);
                    lastKnownState = false;
                }

            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }
    }
}
