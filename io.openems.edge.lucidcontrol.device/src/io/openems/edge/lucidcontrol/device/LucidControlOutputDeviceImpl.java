package io.openems.edge.lucidcontrol.device;


import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlDeviceOutput;

import io.openems.edge.lucidcontrol.device.task.LucidControlOutputTask;
import org.osgi.service.cm.ConfigurationException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of an OutputDevice connected to the LucidControl Output Module.
 * You can configure a device here and set the Output via the Percentage Channel.
 * The Task will be handled by the LucidControlBridge.
 */
@Designate(ocd = OutputConfig.class, factory = true)
@Component(name = "Device.LucidControl.Output")
public class LucidControlOutputDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, LucidControlDeviceOutput {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;
    @Reference
    ComponentManager cpm;

    private final Map<Double, Double> voltageThresholdMap = new HashMap<>();
    private final List<Double> keyList = new ArrayList<>();

    public LucidControlOutputDeviceImpl() {
        super(OpenemsComponent.ChannelId.values(),
                LucidControlDeviceOutput.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, OutputConfig config) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.allocateValuesForMap(config.voltageThreshold(), config.voltageThresholdValue());
        this.lucidControlBridge.addLucidControlTask(config.id(),
                new LucidControlOutputTask(config.moduleId(), config.id(), this.lucidControlBridge.getPath(config.moduleId()),
                        this.lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(),
                        ChannelAddress.fromString(super.id() + "/" + getPercentageChannel().channelId().id()),
                        this.keyList, this.voltageThresholdMap, this.cpm));
    }

    /**
     * In and Outputs having a small offset; that means if you write 5.5V an input device can only read 5.2 V for example
     * Therefore you can set up an Offset that will be added to the Output.
     *
     * @param voltageThreshold      the input %
     * @param voltageThresholdValue the offset added to the %
     * @throws ConfigurationException if the length of thresholds differ
     */
    private void allocateValuesForMap(double[] voltageThreshold, double[] voltageThresholdValue) throws ConfigurationException {
        if (voltageThreshold.length > voltageThresholdValue.length) {
            throw new ConfigurationException("Config Voltage Threshold and Their Values", "Not enough Thresholds!");
        }
        for (int x = 0; x < voltageThreshold.length; x++) {
            this.keyList.add(x, voltageThreshold[x]);
            this.voltageThresholdMap.put(voltageThreshold[x], voltageThresholdValue[x]);
        }
    }

    @Deactivate
    protected void deactivate() {
        this.lucidControlBridge.removeTask(super.id());
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, OutputConfig config) throws OpenemsError.OpenemsNamedException {
        boolean idChanged = super.id().equals(config.id()) == false;
        if (idChanged) {
            this.lucidControlBridge.removeTask(super.id());
        }
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (idChanged) {
            this.lucidControlBridge.addLucidControlTask(config.id(),
                    new LucidControlOutputTask(config.moduleId(), config.id(), this.lucidControlBridge.getPath(config.moduleId()),
                            this.lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(),
                            ChannelAddress.fromString(super.id() + "/" + getPercentageChannel().channelId().id()),
                            this.keyList, this.voltageThresholdMap, this.cpm));
        }
    }

    @Override
    public String debugLog() {
        if (this.getPercentageValue().isDefined()) {
            return "The pressure of " + super.id() + " is set to: " + this.getPercentageValue().get();
        } else {
            return "The pressure of " + super.id() + " was not set yet";
        }
    }

}
