package io.openems.edge.lucidcontrol.device;


import io.openems.edge.bridge.lucidcontrol.api.LucidControlBridge;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.lucidcontrol.device.api.LucidControlDeviceOutput;

import io.openems.edge.lucidcontrol.device.task.LucidControlOutputTask;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Designate(ocd = OutputConfig.class, factory = true)
@Component(name = "Device.LucidControl.Output")
public class LucidControlOutputDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, LucidControlDeviceOutput {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.MANDATORY)
    LucidControlBridge lucidControlBridge;

    private Map<Double, Double> voltageThresholdMap = new HashMap<>();
    private List<Double> keyList = new ArrayList<>();

    public LucidControlOutputDeviceImpl() {
        super(OpenemsComponent.ChannelId.values(),
                LucidControlDeviceOutput.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, OutputConfig config) throws ConfigurationException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        allocateValuesForMap(config.voltageThreshold(), config.voltageThresholdValue());
        lucidControlBridge.addLucidControlTask(config.id(),
                new LucidControlOutputTask(config.moduleId(), config.id(), lucidControlBridge.getPath(config.moduleId()),
                        lucidControlBridge.getVoltage(config.moduleId()), config.pinPos(),
                        this.getPercentageChannel(), keyList, voltageThresholdMap));
    }

    private void allocateValuesForMap(double[] voltageThreshold, double[] voltageThresholdValue) throws ConfigurationException {
        if (voltageThreshold.length > voltageThresholdValue.length) {
            throw new ConfigurationException("Config Voltage Threshold and Their Values", "Not enough Thresholds!");
        }
        for (int x = 0; x < voltageThreshold.length; x++) {
            this.keyList.add(x, voltageThreshold[x]);
            voltageThresholdMap.put(voltageThreshold[x], voltageThresholdValue[x]);
        }
    }

    @Deactivate
    public void deactivate() {
        lucidControlBridge.removeTask(super.id());
        super.deactivate();
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
