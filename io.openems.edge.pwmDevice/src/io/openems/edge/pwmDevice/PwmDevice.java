package io.openems.edge.pwmDevice;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridgei2c.I2cBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pwmDevice.task.PwmDeviceTaskImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "PwmDevice")
public class PwmDevice extends AbstractOpenemsComponent implements OpenemsComponent, PwmPowerLevelChannel {

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
//        this.pulseDuration = config.pwm_pulseDuration();
        this.isInverse = config.isInverse();
        this.initialValue = config.percentage_Initial();

        try {
            if (cpm.getComponent(i2cBridge) instanceof I2cBridge) {
                I2cBridge i2c = cpm.getComponent(i2cBridge);
                i2c.addI2cTask(super.id(), new PwmDeviceTaskImpl(super.id(), this.getPwmPowerLevelChannel(), pwmModule, pinPosition, isInverse, initialValue));
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }

    }

    @Deactivate
    public void deactivate() {
        try {
            if (cpm.getComponent(i2cBridge) instanceof I2cBridge) {
                I2cBridge i2c = cpm.getComponent(i2cBridge);
                i2c.removeI2cTask(super.id());
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
        super.deactivate();
    }


}
