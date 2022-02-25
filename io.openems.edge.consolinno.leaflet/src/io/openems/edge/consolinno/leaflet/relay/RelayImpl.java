package io.openems.edge.consolinno.leaflet.relay;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.core.api.Error;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
import io.openems.edge.io.api.Relay;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a Relay from the Consolinno Relay Module.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Leaflet.Relay")
public class RelayImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Relay, ModbusComponent {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private LeafletCore lc;
    private final Logger log = LoggerFactory.getLogger(RelayImpl.class);
    private int relayModule;
    private int position;
    private int relayDiscreteOutput;
    private boolean inverted;

    public RelayImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Relay.ChannelId.values(),
                ModbusComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist! Check Config!");
        }
        this.relayModule = config.module();
        this.position = config.position();
        this.inverted = config.isInverse();
        //Check if the Module is physically present, else throws ConfigurationException.
        if (this.lc.modbusModuleCheckout(LeafletCore.ModuleType.REL, config.module(), config.position(), config.id())
                // Position is not the Correct Mreg Number, because Output coils start at 0 and not 1 like Analog Input
                && (this.lc.getFunctionAddress(LeafletCore.ModuleType.REL, this.relayModule, (this.position - 1)) != Error.ERROR.getValue())) {
            // Inverts Relay, if it is Configured and able to do so.
            this.relayDiscreteOutput = this.lc.getFunctionAddress(LeafletCore.ModuleType.REL, this.relayModule, (this.position - 1));
            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());
        } else {
            throw new ConfigurationException("Relay Module not configured properly. Please check the Config",
                    "This Relay doesn't exist");
        }
    }

    @Deactivate
    protected void deactivate() {
        try {
            this.getRelaysWriteChannel().setNextWriteValue(false);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in getRelaysWriteChannel.setNextWriteValue");
        }
        this.lc.removeModule(LeafletCore.ModuleType.REL, this.relayModule, this.position);
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        if (this.lc.checkFirmwareCompatibility()) {
            return new ModbusProtocol(this,
                    new FC5WriteCoilTask(this.relayDiscreteOutput,
                            (ModbusCoilElement) m(Relay.ChannelId.WRITE_ON_OFF, new CoilElement(this.relayDiscreteOutput),
                                    ElementToChannelConverter.INVERT_IF_TRUE(this.inverted))),
                    new FC1ReadCoilsTask(this.relayDiscreteOutput, Priority.HIGH,
                            m(Relay.ChannelId.READ_ON_OFF, new CoilElement(this.relayDiscreteOutput),
                                    ElementToChannelConverter.INVERT_IF_TRUE(this.inverted))));
        } else {
            this.deactivate();
            return null;
        }
    }

    @Override
    public String debugLog() {
        if (this.getRelaysWriteChannel().getNextWriteValue().isPresent()) {
            if (this.getRelaysReadChannel().value().isDefined()) {
                return "Write: " + this.getRelaysWriteChannel().getNextWriteValue().get()
                        + " Read: " + this.getRelaysReadChannel().value().get();
            }
            return this.getRelaysWriteChannel().getNextWriteValue().get().toString();
        }
        return "No ON_OFF value";
    }
}
