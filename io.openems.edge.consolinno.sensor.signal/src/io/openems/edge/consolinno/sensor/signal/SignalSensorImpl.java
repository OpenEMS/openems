package io.openems.edge.consolinno.sensor.signal;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
import io.openems.edge.consolinno.sensor.signal.api.SignalSensor;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.signal.sensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class SignalSensorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Thermometer, SignalSensor, EventHandler {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletConfigurator lc;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    //Decimal Degrees
    private static final int maxTemperature = 1000;

    private int signalModule;
    private int position;
    private int temperatureAnalogInput;
    private boolean isInverted;

    public SignalSensorImpl() {
        super(OpenemsComponent.ChannelId.values(), Thermometer.ChannelId.values(), SignalSensor.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        this.signalModule = config.module();
        this.position = config.position();
        this.isInverted = config.inverted();
        this.getSignalType().setNextValue(config.signalType());
        //Check if the Module is physically present, else throws ConfigurationException.
        if (lc.modbusModuleCheckout(LeafletConfigurator.ModuleType.TMP, config.module(), config.position(), config.id())
                && (lc.getFunctionAddress(LeafletConfigurator.ModuleType.TMP, this.signalModule, this.position) != 65535)) {
            this.temperatureAnalogInput = lc.getFunctionAddress(LeafletConfigurator.ModuleType.TMP, this.signalModule, this.position);

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Signal sensor not configured properly. Please check the Config", "This Sensor doesn't Exist");
        }
    }

    @Deactivate
    public void deactivate() {
        lc.removeModule(LeafletConfigurator.ModuleType.TMP, this.signalModule, this.position);
        super.deactivate();

    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(this.temperatureAnalogInput, Priority.HIGH,
                        m(Thermometer.ChannelId.TEMPERATURE, new UnsignedWordElement(this.temperatureAnalogInput),
                                ElementToChannelConverter.DIRECT_1_TO_1)));

    }

    @Override
    public String debugLog() {
        return "Temperature " + getTemperatureValue() + " Signal: " + getSignalType().value();
    }

    @Override
    public void handleEvent(Event event) {
        if (getSignalType().value().isDefined()) {
            if (isInverted) {
                if (getTemperatureValue() < maxTemperature) {
                    getSignalType().setNextValue("Error");
                }
            } else if (!isInverted) {
                if (getTemperatureValue() > maxTemperature) {
                    getSignalType().setNextValue("Error");
                } else if (!getSignalType().value().get().equals("Status")) {
                    getSignalType().setNextValue("Status");
                }
            }
        }
    }
}
