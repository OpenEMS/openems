package io.openems.edge.consolinno.relay;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
import io.openems.edge.relay.api.Relay;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.relay")
public class RelayImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Relay {

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletConfigurator lc;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private final Logger log = LoggerFactory.getLogger(RelayImpl.class);
    private int relayModule;
    private int position;
    private int relayDiscreteOutput;
    private boolean inverted;
    public RelayImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Relay.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        this.relayModule = config.module();
        this.position = config.position();
         this.inverted = config.isInverse();
        //Check if the Module is physically present, else throws ConfigurationException.
        if (lc.modbusModuleCheckout(LeafletConfigurator.ModuleType.REL, config.module(), config.position(), config.id())
                //Position is not the Correct Mreg Number, because Output coils start at 0 and not 1 like Analog Input
                && (lc.getFunctionAddress(LeafletConfigurator.ModuleType.REL, this.relayModule, (this.position - 1)) != 65535)) {
            /* Inverts Relay, if it is Configured and able to do so. */
            this.relayDiscreteOutput = lc.getFunctionAddress(LeafletConfigurator.ModuleType.REL, this.relayModule, (this.position - 1));
            if (this.position >= 5 && this.inverted) {
                lc.invertRelay(this.relayModule, (this.position - 4));
            }
            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());
        } else {
            throw new ConfigurationException("Relay Module not configured properly. Please check the Config",
                    "This Relay doesn't Exist");
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            this.getRelaysWriteChannel().setNextWriteValue(false);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in getRelaysWriteChannel.setNextWriteValue");
        }
        if (this.inverted){
            lc.revertInversion(this.relayModule,(this.position-4));
        }
        lc.removeModule(LeafletConfigurator.ModuleType.REL, this.relayModule, this.position);
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(this,
                new FC5WriteCoilTask(this.relayDiscreteOutput,
                        m(Relay.ChannelId.WRITE_ON_OFF, new CoilElement(this.relayDiscreteOutput),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC1ReadCoilsTask(this.relayDiscreteOutput, Priority.HIGH,
                        m(Relay.ChannelId.READ_ON_OFF, new CoilElement(this.relayDiscreteOutput),
                                ElementToChannelConverter.DIRECT_1_TO_1)));
    }

    @Override
    public String debugLog() {
        if (this.getRelaysWriteChannel().getNextWriteValue().isPresent()) {
            if (this.getRelaysReadChannel().value().isDefined()) {
                return "Write: " + this.getRelaysWriteChannel().getNextWriteValue().get().toString() + " Read: " + this.getRelaysReadChannel().getNextValue().get().toString();
            }
            return this.getRelaysWriteChannel().getNextWriteValue().get().toString();
        }
        return "No ON_OFF value";
    }
}
