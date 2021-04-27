package io.openems.edge.lucidcontrol.device;

import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.lucidcontrol.device.api.LucidControlDeviceInput;
import io.openems.edge.lucidcontrol.device.task.LucidControlInputTask;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = InputConfig.class, factory = true)
@Component(name = "Device.LucidControl.Input")
public class LucidControlInputDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, LucidControlDeviceInput {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;


    public LucidControlInputDeviceImpl() {
        super(OpenemsComponent.ChannelId.values(),
                LucidControlDeviceInput.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, InputConfig config) {

        super.activate(context, config.id(), config.alias(), config.enabled());
        lucidControlBridge.addLucidControlTask(config.id(),
                new LucidControlInputTask(config.moduleId(), config.id(),
                        lucidControlBridge.getPath(config.moduleId()),
                        lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(), config.maxPressure(),
                        this.getPressureChannel(), this.getVoltageChannel()));
    }

    @Deactivate
    public void deactivate() {
        lucidControlBridge.removeTask(super.id());
        super.deactivate();
    }

    @Override
    public String debugLog() {
        if (getPressure().isDefined() && getVoltage().isDefined()) {
            return "The pressure of " + super.id() + " is: " + this.getPressure().get() + "\nVoltage Read: " + this.getVoltage().get();
        } else {
            return "The pressure of " + super.id() + " is not defined yet.";
        }
    }
}
