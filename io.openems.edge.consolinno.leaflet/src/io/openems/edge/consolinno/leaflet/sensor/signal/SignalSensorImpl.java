package io.openems.edge.consolinno.leaflet.sensor.signal;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
import io.openems.edge.consolinno.leaflet.sensor.signal.api.SignalSensor;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.consolinno.leaflet.core.api.Error;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a Consolinno Signal sensor. Connected to the Temperature Module.
 * When the Sensor detects a Temperature above 100 °C (Analogue Signal can be read) it will output an SignalActive (true).
 * If the Logic is inverted by the config. It showcases an SignalActive(true) when the AnalogueSignal is missing e.g. Temperature < 100 °C.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Leaflet.Sensor.Signal", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class SignalSensorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Thermometer, SignalSensor, EventHandler, ModbusComponent {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    //Decimal Degrees
    private static final int MAX_TEMPERATURE = 1000;
    private LeafletCore lc;
    private final Logger log = LoggerFactory.getLogger(SignalSensorImpl.class);
    private int signalModule;
    private int position;
    private int temperatureAnalogInput;
    private boolean isInverted;

    public SignalSensorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Thermometer.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                SignalSensor.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist! Check Config!");
        }
        this.signalModule = config.module();
        this.position = config.position();
        this.isInverted = config.inverted();
        this.getSignalType().setNextValue(config.signalType().name());
        //Check if the Module is physically present, else throws ConfigurationException.
        if (this.lc.modbusModuleCheckout(LeafletCore.ModuleType.TMP, config.module(), config.position(), config.id())
                && (this.lc.getFunctionAddress(LeafletCore.ModuleType.TMP, this.signalModule, this.position) != Error.ERROR.getValue())) {
            this.temperatureAnalogInput = this.lc.getFunctionAddress(LeafletCore.ModuleType.TMP, this.signalModule, this.position);

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Signal sensor not configured properly. Please check the Config", "This Sensor doesn't Exist");
        }
    }

    @Deactivate
    protected void deactivate() {
        this.lc.removeModule(LeafletCore.ModuleType.TMP, this.signalModule, this.position);
        super.deactivate();

    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        if (this.lc.checkFirmwareCompatibility()) {
            return new ModbusProtocol(this,
                    new FC4ReadInputRegistersTask(this.temperatureAnalogInput, Priority.HIGH,
                            m(Thermometer.ChannelId.TEMPERATURE, new SignedWordElement(this.temperatureAnalogInput),
                                    ElementToChannelConverter.DIRECT_1_TO_1)));
        } else {
            return null;
        }
    }

    @Override
    public String debugLog() {
        return "SignalStatus: " + signalActive().value().orElse(false) + " SignalType: " + getSignalType().value();
    }

    @Override
    public void handleEvent(Event event) {
        Value<Integer> currentTemperature = getTemperature();
        boolean currentTempDefined = currentTemperature.isDefined();
        boolean signalActive = false;
        if (currentTempDefined) {
            signalActive = this.isInverted == (currentTemperature.get() < MAX_TEMPERATURE);
        }
        this.signalActive().setNextValue(signalActive);
    }

}
