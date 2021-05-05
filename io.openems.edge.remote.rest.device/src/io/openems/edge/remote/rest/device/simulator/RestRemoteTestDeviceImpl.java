package io.openems.edge.remote.rest.device.simulator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This class can be used to try out Read and Write of the RestRemoteDeviceImpl.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Rest.Remote.Device.Test", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class RestRemoteTestDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteTestDevice, EventHandler {

    public RestRemoteTestDeviceImpl() {
        super(OpenemsComponent.ChannelId.values(),
                RestRemoteDevice.ChannelId.values());
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
    public void handleEvent(Event event) {
        if (EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE.equals(event.getTopic())) {
            if (this.getWriteValueChannel().getNextWriteValue().isPresent()) {
                this.getReadValueChannel().setNextValue(this.getWriteValueChannel().getNextWriteValue().get());
            }
        }
    }
}
