package io.openems.edge.controller.heatnetwork.controlcenter.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Dummy.Center")
public class DummyControlCenter extends AbstractOpenemsComponent implements OpenemsComponent, ControlCenter, EventHandler {

    @Reference
    ComponentManager cpm;

    private Relay relaysChannel;

    public DummyControlCenter() {
        super(OpenemsComponent.ChannelId.values(),
                ControlCenter.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.relaysChannel = cpm.getComponent(config.relay()) instanceof Relay ? cpm.getComponent(config.relay()) : null;
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        if (this.activateTemperatureOverride().value().isDefined() && this.setOverrideTemperature().value().isDefined()) {
            return this.activateTemperatureOverride().value().get() + " \n"
                    + this.setOverrideTemperature().value().get() + "\n";
        }

        return "Nothing done yet";
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.activateTemperatureOverride().value().isDefined() && this.activateTemperatureOverride().value().get()) {
                try {
                    this.relaysChannel.getRelaysWriteChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
