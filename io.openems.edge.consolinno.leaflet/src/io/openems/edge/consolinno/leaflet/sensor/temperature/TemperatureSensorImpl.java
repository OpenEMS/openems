package io.openems.edge.consolinno.leaflet.sensor.temperature;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a Consolinno Temperature sensor. It communicates via Modbus with the Temperature Module, gets it's addresses
 * via the LeafletCore and sets it's Temperature into the Thermometer Nature.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Consolinno.Leaflet.Temperature", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSensorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Thermometer, ModbusComponent {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private LeafletCore lc;
    private final Logger log = LoggerFactory.getLogger(TemperatureSensorImpl.class);
    private int temperatureModule;
    private int position;
    private int temperatureAnalogInput;

    public TemperatureSensorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                Thermometer.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist! Check Config!");
        }
        this.temperatureModule = config.module();
        this.position = config.position();
        //Check if the Module is physically present, else throws ConfigurationException.
        if (this.lc.modbusModuleCheckout(LeafletCore.ModuleType.TMP, config.module(), config.position(), config.id())
                && (this.lc.getFunctionAddress(LeafletCore.ModuleType.TMP, this.temperatureModule, this.position) != 65535)) {
            this.temperatureAnalogInput = this.lc.getFunctionAddress(LeafletCore.ModuleType.TMP, this.temperatureModule, this.position);

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Temperature Module not configured properly. Please check the Config", "This Sensor doesn't Exist");
        }
    }

    @Deactivate
    protected void deactivate() {
        this.lc.removeModule(LeafletCore.ModuleType.TMP, this.temperatureModule, this.position);
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
            this.deactivate();
            return null;
        }
    }

    @Override
    public String debugLog() {
        String temperature = getTemperature().isDefined() ? getTemperature().get().toString()
                + getTemperatureChannel().channelDoc().getUnit().getSymbol() : "Not Defined";
        return "Temperature: " + temperature;
    }

}
