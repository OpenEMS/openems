package io.openems.edge.consolinno.pwm;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
import io.openems.edge.pwm.api.Pwm;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.pwm", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PwmImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Pwm {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletConfigurator lc;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private final Logger log = LoggerFactory.getLogger(PwmImpl.class);
    private int pwmModule;
    private int position;
    private int pwmAnalogOutput;
    private int pwmDiscreteOutput;

    public PwmImpl() {
        super(OpenemsComponent.ChannelId.values(), Pwm.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        this.pwmModule = config.module();
        this.position = config.position();
        //Check if the Module is physically present, else throws ConfigurationException.
        if (lc.modbusModuleCheckout(LeafletConfigurator.ModuleType.PWM, config.module(), config.position(), config.id())
                && (lc.getFunctionAddress(LeafletConfigurator.ModuleType.PWM, this.pwmModule, this.position) != 65535)) {
            this.pwmAnalogOutput = lc.getFunctionAddress(LeafletConfigurator.ModuleType.PWM, this.pwmModule, this.position);
            //The mReg number for the DiscreteOutput is not the Position but one less.
            this.pwmDiscreteOutput = lc.getPwmDiscreteOutputAddress(this.pwmModule, (this.position - 1));

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Pwm not configured properly. Please check the Config", "This Device doesn't Exist");
        }
        try {
            getWritePwmPowerLevelChannel().setNextWriteValue((float) config.percent());
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in getWritePwmPowerChannel.setNextWriteValue");
        }
        try {
            getInvertedStatus().setNextWriteValue(config.isInverted());
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in getInvertStatus.setNextWriteValue");
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            getWritePwmPowerLevelChannel().setNextWriteValue((float) 0);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in getWritePwmPowerChannel.setNextWriteValue");
        }
        lc.removeModule(LeafletConfigurator.ModuleType.PWM, this.pwmModule, this.position);
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(this.pwmAnalogOutput, Priority.HIGH,
                        m(Pwm.ChannelId.READ_POWER_LEVEL, new UnsignedWordElement(this.pwmAnalogOutput),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(this.pwmAnalogOutput,
                        m(Pwm.ChannelId.WRITE_POWER_LEVEL,
                                new SignedWordElement(this.pwmAnalogOutput),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC5WriteCoilTask(this.pwmDiscreteOutput,
                        m(Pwm.ChannelId.INVERTED,
                                new CoilElement(this.pwmDiscreteOutput),
                                ElementToChannelConverter.DIRECT_1_TO_1)));
    }

    @Override
    public String debugLog() {
        return "Power Level: " + getPowerLevelValue();
    }
}
