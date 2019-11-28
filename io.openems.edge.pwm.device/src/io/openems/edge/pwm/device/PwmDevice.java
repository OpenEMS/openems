package io.openems.edge.pwm.device;

import io.openems.edge.pwm.device.task.PwmDeviceTaskImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.i2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PwmDevice",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class PwmDevice extends AbstractOpenemsComponent implements OpenemsComponent, PwmPowerLevelChannel {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    I2cBridge refI2cBridge;

    public PwmDevice() {
        super(OpenemsComponent.ChannelId.values(), PwmPowerLevelChannel.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {

        super.activate(context, config.id(), config.alias(), config.enabled());

        try {

            refI2cBridge.addI2cTask(super.id(), new PwmDeviceTaskImpl(super.id(), this.getPwmPowerLevelChannel(), config.pwm_module(), config.pinPosition(), config.isInverse()));

        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Deactivate
    public void deactivate() {
        refI2cBridge.removeI2cTask(super.id());
        super.deactivate();
    }

}
