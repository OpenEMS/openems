package io.openems.edge.heater.chp.viessmann;


import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.aio.api.AioChannel;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.ChpBasic;
import io.openems.edge.heater.ChpPower;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.heater.chp.viessmann.api.ViessmannInformation;
import io.openems.edge.heater.chp.viessmann.api.ViessmannPowerPercentage;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Chp",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class ChpImplViessmann extends AbstractOpenemsModbusComponent implements OpenemsComponent, ViessmannInformation, EventHandler, Heater {

    private final Logger log = LoggerFactory.getLogger(ChpImplViessmann.class);
    private ViessmannChpType chpType;
    private int thermicalOutput;
    private int electricalOutput;
    private Relay relay;
    private boolean useRelay;
    private AccessChp accessChp;
    private boolean hadError;
    private boolean isEnabled;
    private int cycleCounter = 0;
    private AioChannel aioChannel;

    private Config config;
    private boolean wasActiveBefore;

    private String[] errorPossibilities = ErrorPossibilities.STANDARD_ERRORS.getErrorList();

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ChpImplViessmann() {
        super(OpenemsComponent.ChannelId.values(),
                ViessmannPowerPercentage.ChannelId.values(),
                ViessmannInformation.ChannelId.values(),
                ChpBasic.ChannelId.values(),
                ChpPower.ChannelId.values(),
                Heater.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());
        this.config = config;
        switch (config.chpType()) {
            case "EM_6_15":
                this.chpType = ViessmannChpType.Vito_EM_6_15;
                break;
            case "EM_9_20":
                this.chpType = ViessmannChpType.Vito_EM_9_20;
                break;
            case "EM_20_39":
                this.chpType = ViessmannChpType.Vito_EM_20_39;
                break;
            case "EM_20_39_70":
                this.chpType = ViessmannChpType.Vito_EM_20_39_RL_70;
                break;
            case "EM_50_81":
                this.chpType = ViessmannChpType.Vito_EM_50_81;
                break;
            case "EM_70_115":
                this.chpType = ViessmannChpType.Vito_EM_70_115;
                break;
            case "EM_100_167":
                this.chpType = ViessmannChpType.Vito_EM_100_167;
                break;
            case "EM_140_207":
                this.chpType = ViessmannChpType.Vito_EM_140_207;
                break;
            case "EM_199_263":
                this.chpType = ViessmannChpType.Vito_EM_199_263;
                break;
            case "EM_199_293":
                this.chpType = ViessmannChpType.Vito_EM_199_293;
                break;
            case "EM_238_363":
                this.chpType = ViessmannChpType.Vito_EM_238_363;
                break;
            case "EM_363_498":
                this.chpType = ViessmannChpType.Vito_EM_363_498;
                break;
            case "EM_401_549":
                this.chpType = ViessmannChpType.Vito_EM_401_549;
                break;
            case "EM_530_660":
                this.chpType = ViessmannChpType.Vito_EM_530_660;
                break;
            case "BM_36_66":
                this.chpType = ViessmannChpType.Vito_BM_36_66;
                break;
            case "BM_55_88":
                this.chpType = ViessmannChpType.Vito_BM_55_88;
                break;
            case "BM_190_238":
                this.chpType = ViessmannChpType.Vito_BM_190_238;
                break;
            case "BM_366_437":
                this.chpType = ViessmannChpType.Vito_BM_366_437;
                break;

            default:
                break;

        }
        this.accessChp = AccessChp.READ;
        this.useRelay = config.useRelay();
        if (config.accesMode().equals("rw")) {
            this.accessChp = AccessChp.READWRITE;
            if (cpm.getComponent(config.chpModuleId()) instanceof AioChannel) {
                this.aioChannel = cpm.getComponent(config.chpModuleId());
                ;
                //TODO
                //mcp.addTask(super.id(), new ChpTaskImpl(super.id(),
                //      config.position(), config.minLimit(), config.maxLimit(),
                //     config.percentageRange(), 4096.f, this.getPowerLevelChannel()));
            }
            if (this.useRelay == true) {
                if (cpm.getComponent(config.relayId()) instanceof Relay) {
                    this.relay = cpm.getComponent(config.relayId());
                    this.relay.getRelaysWriteChannel().setNextWriteValue(false);
                }

                if (config.startOnActivation()) {
                    this.getPowerLevelChannel().setNextValue(config.startPercentage());
                    if (this.useRelay) {
                        this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                    }
                }

            }
        }
        this.thermicalOutput = Math.round(this.chpType.getThermalOutput());
        this.electricalOutput = Math.round(this.chpType.getElectricalOutput());
        this.getStateChannel().setNextValue(HeaterState.AWAIT);
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        if (this.accessChp == AccessChp.READWRITE) {
            //TODO
            // this.mcp.removeTask(super.id());
        }
        if (this.useRelay) {
            try {
                this.relay.getRelaysWriteChannel().setNextWriteValue(false);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public String debugLog() {
        //getInformations();
        if (this.accessChp == AccessChp.READWRITE) {
            if (this.getPowerLevelChannel().getNextValue().get() != null) {
                if (chpType != null) {
                    return "Chp: " + this.chpType.getName() + "is at " + this.getPowerLevelChannel().getNextValue().get()
                            + "\nErrors in Chp: "
                            + this.getErrorChannel().getNextValue().toString() + "\n";
                } else {
                    return "Chp is at " + this.getPowerLevelChannel().getNextValue().get() + "\nErrors in Chp: "
                            + this.getErrorChannel().getNextValue().toString() + "\n";
                }
            }
            return "Percentage Level at 0\n";
        } else {
            return this.getErrorChannel().getNextValue().get() + "\n";
        }
    }


    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(0x4000, Priority.LOW,
                        new DummyRegisterElement(0x4000, 0x4000),
                        m(ViessmannInformation.ChannelId.MODE, new UnsignedWordElement(0x4001),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.STATUS, new UnsignedWordElement(0x4002),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_MODE, new UnsignedWordElement(0x4003),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SET_POINT_OPERATION_MODE, new SignedWordElement(0x4004),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_1, new UnsignedWordElement(0x4005),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_2, new UnsignedWordElement(0x4006),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_3, new UnsignedWordElement(0x4007),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_4, new UnsignedWordElement(0x4008),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_5, new UnsignedWordElement(0x4009),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_6, new UnsignedWordElement(0x400A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_7, new UnsignedWordElement(0x400B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_8, new UnsignedWordElement(0x400C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_HOURS, new UnsignedWordElement(0x400D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_MINUTES, new UnsignedWordElement(0x400E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.START_COUNTER, new UnsignedWordElement(0x400F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.MAINTENANCE_INTERVAL, new SignedWordElement(0x4010),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.MODULE_LOCK, new SignedWordElement(0x4011),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.WARNING_TIME, new SignedWordElement(0x4012),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.NEXT_MAINTENANCE, new UnsignedWordElement(0x4013),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_A, new SignedWordElement(0x4014),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_B, new SignedWordElement(0x4015),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_C, new SignedWordElement(0x4016),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_D, new SignedWordElement(0x4017),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_1, new SignedWordElement(0x4018),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_2, new SignedWordElement(0x4019),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_3, new SignedWordElement(0x401A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_4, new SignedWordElement(0x401B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_5, new SignedWordElement(0x401C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_6, new SignedWordElement(0x401D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x401E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OIL_PRESSURE, new SignedWordElement(0x401F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.LAMBDA_PROBE_VOLTAGE, new SignedWordElement(0x4020),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4025, Priority.LOW,
                        m(ViessmannInformation.ChannelId.ROTATION_PER_MIN, new UnsignedWordElement(0x4025),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.TEMPERATURE_CONTROLLER, new SignedWordElement(0x4026),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.TEMPERATURE_CLEARANCE, new SignedWordElement(0x4027),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L1, new SignedWordElement(0x4028),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L2, new SignedWordElement(0x4029),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L3, new SignedWordElement(0x402A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L1, new SignedWordElement(0x402B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L2, new SignedWordElement(0x402C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L3, new SignedWordElement(0x402D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L1, new SignedWordElement(0x402E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L2, new SignedWordElement(0x402F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L3, new SignedWordElement(0x4030),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_TOTAL, new SignedWordElement(0x4031),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_TOTAL, new SignedWordElement(0x4032),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_TOTAL, new SignedWordElement(0x4033),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ENGINE_PERFORMANCE, new SignedWordElement(0x4034),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_FREQUENCY, new FloatDoublewordElement(0x4035),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4037, Priority.LOW,
                        m(ViessmannInformation.ChannelId.GENERATOR_FREQUENCY, new FloatDoublewordElement(0x4037),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x403B, Priority.LOW,
                        m(ViessmannInformation.ChannelId.ACTIVE_POWER_FACTOR, new SignedWordElement(0x403B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.RESERVE, new UnsignedDoublewordElement(0x403C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(0x403E, 0x403E)));

    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        //percent
        if (this.accessChp.equals(AccessChp.READWRITE) && isEnabled) {
            if (this.isErrorOccured().value().isDefined() && this.isErrorOccured().value().get()) {
                this.setState(HeaterState.ERROR.name());
                return 0;
            }
            int providedPower = Math.round(((demand * bufferValue) * 100) / thermicalOutput);
            if (this.useRelay == true) {
                this.relay.getRelaysWriteChannel().setNextWriteValue(true);
            }


            if (providedPower >= 100) {

                getPowerLevelChannel().setNextWriteValue(100);
                return thermicalOutput;

            } else {
                getPowerLevelChannel().setNextWriteValue(providedPower);
                providedPower = providedPower < this.config.startPercentage() ? config.startPercentage() : providedPower;
                return (providedPower * thermicalOutput) / 100;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getMaximumThermalOutput() {
        return thermicalOutput;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        if (this.useRelay == true) {
            this.relay.getRelaysWriteChannel().setNextWriteValue(false);
        }
        getPowerLevelChannel().setNextValue(0);
        this.getStateChannel().setNextValue(HeaterState.OFFLINE.name());
    }

    @Override
    public boolean hasError() {
        if (this.isErrorOccured().value().isDefined()) {
            return this.isErrorOccured().value().get();
        } else {
            return false;
        }
    }

    @Override
    public void requestMaximumPower() {
        if (isEnabled) {
            if (this.useRelay == true) {
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    log.warn("couldn't Open Relay " + e.getMessage());
                    this.setState(HeaterState.ERROR.name());
                    return;
                }
            }
            this.getPowerLevelChannel().setNextValue(100);
            this.setState(HeaterState.RUNNING.name());
        }
    }

    @Override
    public void setIdle() {
        if (isEnabled && errorInHeater() == false) {
            this.setState(HeaterState.AWAIT.name());
            if (this.accessChp.equals(AccessChp.READWRITE)) {
                this.getPowerLevelChannel().setNextValue(0);
            }
        }

    }

    private void forever() {
        List<String> errorSummary = new ArrayList<>();

        char[] allErrorsAsChar = generateErrorAsCharArray();

        int errorMax = 80;
        //int errorBitLength = 16;
        for (int i = 0, errorListPosition = 0; i < errorMax; i++) {
            if (allErrorsAsChar[i] == '1') {
                if (errorPossibilities[i].toLowerCase().contains("reserve")) {
                    errorListPosition++;
                } else {
                    errorSummary.add(errorListPosition, errorPossibilities[i]);
                    errorListPosition++;
                }
            }
        }
        //All occuring errors in openemsChannel.

        if ((errorSummary.size() > 0)) {
            getErrorChannel().setNextValue(errorSummary.toString());
            isErrorOccured().setNextValue(true);
            this.getStateChannel().setNextValue(HeaterState.ERROR);
            this.hadError = true;
        } else {
            getErrorChannel().setNextValue("No Errors found.");
            isErrorOccured().setNextValue(false);
            if (hadError) {
                hadError = false;
                this.setState(HeaterState.AWAIT.name());
            }
        }
        //TODO HOW TO HANDLE ERRORS?

    }

    private char[] generateErrorAsCharArray() {

        String errorBitsAsString = "";
        String dummyString = "0000000000000000";
        if (getErrorOne().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorOne().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorTwo().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorTwo().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorThree().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorThree().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorFour().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorFour().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorFive().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorFive().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorSix().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorSix().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorSeven().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorSeven().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorEight().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorEight().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }

        return errorBitsAsString.toCharArray();
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.forever();
            this.channelmapping();
        }
    }

    protected void channelmapping() {
        // Decide state of enabledSignal.
        // The method isEnabledSignal() does get and reset. Calling it will clear the value (for that cycle). So you
        // need to store the value in a local variable.
        Optional<Boolean> enabledSignal = isEnabledSignal();
        if (enabledSignal.isPresent()) {
            isEnabled = enabledSignal.get();
            cycleCounter = 0;
        } else {
            // No value in the Optional.
            // Wait 5 cycles. If isEnabledSignal() has not been filled with a value again, switch to false.
            if (isEnabled) {
                cycleCounter++;
                if (cycleCounter > 5) {
                    isEnabled = false;
                }
            }
        }
    }
}
