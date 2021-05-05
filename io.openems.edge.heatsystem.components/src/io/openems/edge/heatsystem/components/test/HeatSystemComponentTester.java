package io.openems.edge.heatsystem.components.test;


import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.pwm.api.Pwm;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * This Component allows a Valve  to be configured and controlled.
 * It either works with 2 Relays or 2 ChannelAddresses.
 * It updates it's opening/closing state and shows up the percentage value of itself.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Test.ValveAndPump",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}
)
public class HeatSystemComponentTester extends AbstractOpenemsComponent implements OpenemsComponent, Relay, Pwm, EventHandler {

    private final Logger log = LoggerFactory.getLogger(HeatSystemComponentTester.class);

    public HeatSystemComponentTester() {
        super(OpenemsComponent.ChannelId.values(),
                Relay.ChannelId.values(),
                Pwm.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        return super.id() + " Relay Status: " + this.getRelaysReadChannel().value().orElse(false) + "\n"
                + "Pwm Status: " + this.getReadPwmPowerLevelChannel().value().orElse(0);
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            Optional<Boolean> relay = this.getRelaysWriteChannel().getNextWriteValueAndReset();
            this.getRelaysReadChannel().setNextValue(relay.orElse(false));
            if (relay.isPresent() == false) {
                this.log.warn("Relay Value wasn't set for: " + super.id());
            }
            this.getReadPwmPowerLevelChannel().setNextValue(this.getWritePwmPowerLevelChannel().getNextWriteValueAndReset().orElse(-100));
        }
    }
}
