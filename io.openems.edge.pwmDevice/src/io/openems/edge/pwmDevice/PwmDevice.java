package io.openems.edge.pwmDevice;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pwmDevice.task.PwmDeviceTaskImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PwmDevice",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class PwmDevice extends AbstractOpenemsComponent implements OpenemsComponent, PwmPowerLevelChannel {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    public I2cBridge refI2cBridge;

    @Reference
    ComponentManager cpm;

    private String pwmModule;
    private String i2cBridge;
    private short pinPosition;
    //    private int offset;
//    private int pulseDuration;
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
        // this.pulseDuration = config.pwm_pulseDuration();
        this.isInverse = config.isInverse();
        this.initialValue = config.percentage_Initial();

        //Just temporarly for testing
        this.getPwmPowerLevelChannel().setNextValue(initialValue);

        try {
            refI2cBridge.addI2cTask(super.id(), new PwmDeviceTaskImpl(super.id(), this.getPwmPowerLevelChannel(), pwmModule, pinPosition, isInverse, initialValue));

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
