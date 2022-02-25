package io.openems.edge.consolinno.leaflet.aio;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
import io.openems.edge.io.api.AnalogInputOutput;
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
 * <p>
 * Provides a Consolinno Aio Out/Input.
 * The AIO Module provides the ability to write or read from connections.
 * Setup the Configuration once and you can control devices by sending a Volt or mA value.
 * As well as monitor them by reading the input.
 * Usually you will write a Percent-Value, the BaseSoftware of the Leaflet converts the Percent value to an analogue Signal.
 * </p>
 * <p>
 * NOTE: Since you can damage peripheral devices check your config twice. That's the reason why you cannot "modify" this component,
 * but activate/Deactivate only.
 * </p>
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Leaflet.Aio", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class AioImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, AnalogInputOutput, EventHandler, ModbusComponent {


    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private LeafletCore lc;
    private final Logger log = LoggerFactory.getLogger(AioImpl.class);
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
    private boolean debug;
    private int value;

    public AioImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                AnalogInputOutput.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        try {
            this.lc = this.cpm.getComponent(config.leafletId());
        } catch (Exception e) {
            this.log.error("The LeafletCore doesn't exist! Check Config!");
        }
        this.aioModule = config.module();
        this.position = config.position();
        this.type = config.type();
        this.debug = config.debugValue();
        if (this.debug) {
            this.value = config.value();
        }

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
        if (this.lc.modbusModuleCheckout(LeafletCore.ModuleType.AIO, config.module(), config.position(), config.id())) {
            if (this.type.contains("in")) {
                this.aioRegister = this.lc.getFunctionAddress(LeafletCore.ModuleType.AIO, this.aioModule, this.inputMReg, true);
                this.percentRegister = this.lc.getAioPercentAddress(LeafletCore.ModuleType.AIO, this.aioModule, this.inputPercentMreg, true);
                this.lc.setAioConfig(this.aioModule, this.position, this.type);
            } else if (this.type.contains("out")) {
                this.aioRegister = this.lc.getFunctionAddress(LeafletCore.ModuleType.AIO, this.aioModule, this.outputMReg, false);
                this.percentRegister = this.lc.getAioPercentAddress(LeafletCore.ModuleType.AIO, this.aioModule, this.outputPercentMreg, false);
                this.lc.setAioConfig(this.aioModule, this.position, this.type);
            }

            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());

        } else {
            throw new ConfigurationException("Aio not configured properly. Please check the Config", "This device doesn't exist");
        }
        try {
            this.getWriteChannel().setNextWriteValue(0);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.warn(this.id() + ": Couldn't write into own Channel");
        }

    }

    @Deactivate
    protected void deactivate() {
        this.lc.removeModule(LeafletCore.ModuleType.AIO, this.aioModule, this.position);
        this.lc.setAioConfig(this.aioModule, this.position, "deactivate");
        this.debug = false;
        if (this.type.contains("out")) {
            try {
                this.getWriteChannel().setNextWriteValue(0);
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.log.error("Unable to reset Write Channel.");
            }
        }
        super.deactivate();

    }


    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        if (this.type.equals("Digital_in")) {
            return new ModbusProtocol(this,
                    new FC1ReadCoilsTask(this.aioRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_INPUT, new CoilElement(this.aioRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)),
                    new FC3ReadRegistersTask(this.percentRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_CHECK_THOUSANDTH, new UnsignedWordElement(this.percentRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)));
        } else if (this.type.contains("in")) {
            return new ModbusProtocol(this,
                    new FC4ReadInputRegistersTask(this.aioRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_INPUT, new UnsignedWordElement(this.aioRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)),
                    new FC4ReadInputRegistersTask(this.percentRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_CHECK_THOUSANDTH, new UnsignedWordElement(this.percentRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)));
        } else if (this.type.contains("out")) {
            return new ModbusProtocol(this,
                    new FC6WriteRegisterTask(this.aioRegister,
                            m(AnalogInputOutput.ChannelId.AIO_WRITE,
                                    new UnsignedWordElement(this.aioRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)),
                    new FC3ReadRegistersTask(this.aioRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_CHECK_WRITE, new UnsignedWordElement(this.aioRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)),
                    new FC3ReadRegistersTask(this.percentRegister, Priority.HIGH,
                            m(AnalogInputOutput.ChannelId.AIO_CHECK_THOUSANDTH, new UnsignedWordElement(this.percentRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)),
                    new FC6WriteRegisterTask(this.percentRegister,
                            m(AnalogInputOutput.ChannelId.AIO_THOUSANDTH_WRITE,
                                    new UnsignedWordElement(this.percentRegister),
                                    ElementToChannelConverter.REPLACE_WITH_MINUS_ZERO_IF_0XFFFF)));

        }

        return null;
    }

    @Override
    public String debugLog() {
        if (this.type.contains("Temperature_in")) {
            return this.type + " :" + getInputValue();
        } else if (this.type.contains("in")) {
            return this.type + " :" + getInputValue() + " " + getPercentValue() + "%";
        } else {
            return this.type + " :" + getWriteValue() + " " + getPercentValue() + "%";
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (this.debug) {
            try {
                this.setWriteThousandth(this.value);
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.log.error(this.id() + ": Unable to own write to OutputChannel.");
            }
        }
    }
}
