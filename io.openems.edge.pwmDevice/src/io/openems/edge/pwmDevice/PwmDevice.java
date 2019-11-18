package io.openems.edge.pwmDevice;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Pwm Device")
public class PwmDevice extends AbstractOpenemsComponent implements OpenemsComponent, PwmPowerLevelChannel {

    public PwmDevice() {
        super(OpenemsComponent.ChannelId.values(), PwmPowerLevelChannel.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        //TODO Stuff
    }

    @Deactivate
    public void deactivate() {
    }


}
