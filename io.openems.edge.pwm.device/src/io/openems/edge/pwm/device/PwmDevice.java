package io.openems.edge.pwm.device;

import io.openems.edge.pwm.device.task.PwmDeviceTaskImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridgei2c.I2cBridge;
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

    @Reference
    ComponentManager cpm;

    private String pwmModule;
    private String i2cBridge;
    private short pinPosition;
    private boolean isInverse;
    private float initialValue;

    public PwmDevice() {
        super(OpenemsComponent.ChannelId.values(), PwmPowerLevelChannel.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.pwmModule = config.pwm_module();
        this.i2cBridge = config.i2c_id();
        this.isInverse = config.isInverse();
        this.initialValue = config.percentage_Initial();

        try {

            this.getPwmPowerLevelChannel().setNextValue(initialValue);

            refI2cBridge.addI2cTask(super.id(), new PwmDeviceTaskImpl(super.id(), this.getPwmPowerLevelChannel(), pwmModule, pinPosition, isInverse));

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
