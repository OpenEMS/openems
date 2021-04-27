package io.openems.edge.consolinno.aio;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.aio.api.AioChannel;
import io.openems.edge.consolinno.modbus.configurator.Error;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.aio", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AioImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, AioChannel {
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    LeafletConfigurator lc;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private int aioModule;
    private int position;
    private static final int PERCENTAGE_INPUT_MREG_OFFSET = 0;
    private static final int TEMP_INPUT_MREG_OFFSET = 4;
    private static final int PERCENTAGE_OUTPUT_MREG_OFFSET = 0;
    private static final int DIGITAL_INPUT_MREG_OFFSET = -1;
    private static final int VOLTAGE_INPUT_MREG_OFFSET = 8;
    private static final int CURRENT_INPUT_MREG_OFFSET = 12;
    private static final int VOLTAGE_OUTPUT_MREG_OFFSET = 4;
    private static final int CURRENT_OUTPUT_MREG_OFFSET = 8;
    private int inputPercentMreg;
    private int outputPercentMreg;
    private int inputMReg;
    private int outputMReg;
    private int percentRegister;
    private int aioRegister;
    private String type;

    public AioImpl() {
        super(OpenemsComponent.ChannelId.values(), AioChannel.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.aioModule = config.module();
        this.position = config.position();
        this.type = config.type();
        switch (this.type) {
            case ("0-20mA_in"):
            case ("4-20mA_in"):
                this.inputMReg = this.position + CURRENT_INPUT_MREG_OFFSET;
                this.inputPercentMreg = this.position + PERCENTAGE_INPUT_MREG_OFFSET;
                break;
            case ("10V_in"):
                this.inputMReg = this.position + VOLTAGE_INPUT_MREG_OFFSET;
                this.inputPercentMreg = this.position + PERCENTAGE_INPUT_MREG_OFFSET;
                break;
            case ("Digital_in"):
                this.inputMReg = this.position + DIGITAL_INPUT_MREG_OFFSET;
                this.inputPercentMreg = this.position + PERCENTAGE_INPUT_MREG_OFFSET;
                break;
            case ("Temperature_in"):
                this.inputMReg = this.position + TEMP_INPUT_MREG_OFFSET;
                this.inputPercentMreg = this.position + PERCENTAGE_INPUT_MREG_OFFSET;
                break;
            case ("0-20mA_out"):
            case ("4-20mA_out"):
                this.outputMReg = this.position + CURRENT_OUTPUT_MREG_OFFSET;
                this.outputPercentMreg = this.position + PERCENTAGE_OUTPUT_MREG_OFFSET;
                break;
            case ("10V_out"):
                this.outputMReg = this.position + VOLTAGE_OUTPUT_MREG_OFFSET;
                this.outputPercentMreg = this.position + PERCENTAGE_OUTPUT_MREG_OFFSET;
                break;

        }
        //Check if the Module is physically present, else throws ConfigurationException.
        if (this.lc.modbusModuleCheckout(LeafletConfigurator.ModuleType.AIO, config.module(), config.position(), config.id())
                && (this.lc.getFunctionAddress(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.position) != Error.ERROR.getValue())) {
            if (this.type.contains("in")) {
                this.aioRegister = this.lc.getFunctionAddress(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.inputMReg, true);
                this.percentRegister = this.lc.getAioPercentAddress(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.inputPercentMreg, true);
                this.lc.setAioConfig(this.aioModule, this.inputMReg, this.type);
            } else if (this.type.contains("out")) {
                this.aioRegister = this.lc.getFunctionAddress(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.outputMReg, false);
                this.percentRegister = this.lc.getAioPercentAddress(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.outputPercentMreg, false);
                this.lc.setAioConfig(this.aioModule, this.outputMReg, this.type);
            }

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Aio not configured properly. Please check the Config", "This device doesn't exist");
        }

    }

    @Deactivate
    public void deactivate() {
        this.lc.removeModule(LeafletConfigurator.ModuleType.AIO, this.aioModule, this.position);
        super.deactivate();

    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        if (this.lc.checkFirmwareCompatibility()) {
            if (this.type.equals("Digital_in")) {
                return new ModbusProtocol(this,
                        new FC1ReadCoilsTask(this.aioRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_READ, new CoilElement(this.aioRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)),
                        new FC3ReadRegistersTask(this.percentRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_PERCENT, new UnsignedWordElement(this.percentRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)));
            } else if (this.type.contains("in")) {
                return new ModbusProtocol(this,
                        new FC3ReadRegistersTask(this.aioRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_READ, new UnsignedWordElement(this.aioRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)),
                        new FC3ReadRegistersTask(this.percentRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_PERCENT, new UnsignedWordElement(this.percentRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)));
            } else if (this.type.contains("out")) {
                return new ModbusProtocol(this,
                        new FC6WriteRegisterTask(this.aioRegister,
                                m(AioChannel.ChannelId.AIO_WRITE,
                                        new SignedWordElement(this.aioRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)),
                        new FC3ReadRegistersTask(this.aioRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_CHECK_WRITE, new UnsignedWordElement(this.aioRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)),
                        new FC3ReadRegistersTask(this.percentRegister, Priority.HIGH,
                                m(AioChannel.ChannelId.AIO_PERCENT, new UnsignedWordElement(this.percentRegister),
                                        ElementToChannelConverter.DIRECT_1_TO_1)));


            }
        } else {
            this.deactivate();
            return null;
        }


        return null;
    }

    @Override
    public String debugLog() {
        return "Aio " + this.type + " :" + getReadValue() + getPercentValue() + "%";
    }

}
