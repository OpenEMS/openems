package io.openems.edge.lucidcontrol.device;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.lucidcontrol.api.LucidControlDeviceInput;
import io.openems.edge.lucidcontrol.device.task.LucidControlInputTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This Class is the implementation of a Device that can be configured as an Input on a LucidControl Input Module.
 * It measures the Voltage of the Input device and calculates the pressure depending on the max possible Voltage.
 * Everything will be handled by the Task.
 */
@Designate(ocd = InputConfig.class, factory = true)
@Component(name = "Device.LucidControl.Input")
public class LucidControlInputDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, LucidControlDeviceInput {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;

    @Reference
    ComponentManager cpm;


    public LucidControlInputDeviceImpl() {
        super(OpenemsComponent.ChannelId.values(),
                LucidControlDeviceInput.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, InputConfig config) throws OpenemsError.OpenemsNamedException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.lucidControlBridge.addLucidControlTask(config.id(),
                new LucidControlInputTask(config.moduleId(), config.id(),
                        this.lucidControlBridge.getPath(config.moduleId()),
                        this.lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(), config.maxPressure(),
                        ChannelAddress.fromString(super.id() + "/" + this.getPressureChannel().channelId().id()),
                        ChannelAddress.fromString(super.id() + "/" + this.getVoltageChannel().channelId().id()), this.cpm));
    }

    @Deactivate
    protected void deactivate() {
        this.lucidControlBridge.removeTask(super.id());
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, InputConfig config) throws OpenemsError.OpenemsNamedException {
        boolean idChanged = super.id().equals(config.id()) == false;
        if (idChanged) {
            this.lucidControlBridge.removeTask(super.id());
        }
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (idChanged) {
            this.lucidControlBridge.addLucidControlTask(config.id(),
                    new LucidControlInputTask(config.moduleId(), config.id(),
                            this.lucidControlBridge.getPath(config.moduleId()),
                            this.lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(), config.maxPressure(),
                            ChannelAddress.fromString(config.id() + "/" + this.getPressureChannel().channelId().id()),
                            ChannelAddress.fromString(config.id() + "/" + this.getVoltageChannel().channelId().id()), this.cpm));
        }

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
