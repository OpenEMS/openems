package io.openems.edge.apartmentmodule;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.apartmentmodule.api.ApartmentModule;
import io.openems.edge.apartmentmodule.api.CommunicationCheck;
import io.openems.edge.apartmentmodule.api.OnOff;
import io.openems.edge.apartmentmodule.api.ValveStatus;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import org.joda.time.DateTime;
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

import java.util.Optional;

@Designate(ocd = Config.class, factory = true)
@Component(name = "ApartmentModule",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE})

/**
 * This module reads all variables available via Modbus from a Consolinno Apartment Module and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the Apartment Module via "setNextWriteValue" method.
 */

public class ApartmentModuleImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ApartmentModule {

    private final Logger log = LoggerFactory.getLogger(ApartmentModuleImpl.class);

    private int testcounter = 0;
    private int temperatureCalibration;
    private boolean debugOn;
    private OnOff switchRelay1;
    private OnOff switchRelay2;
    private int relayTime;
    private boolean resetExtReq;
    private boolean topAM;
    private boolean doOnce;
    private int relaysForOpeningHydraulicMixer;
    private static final int DEFAULT_VALVE_PERMANENT = 0;
    private static final int DEFAULT_VALVE_MAX_SECONDS = 100;
    private static final int DEFAULT_VALVE_TIME_FOR_RELAY = DEFAULT_VALVE_MAX_SECONDS * 110;
    private boolean wasDeactivatedBefore = false;
    private boolean relaysShutDown = true;
    private boolean isOpen = false;
    private boolean isClosed = false;

    @Reference
    protected ConfigurationAdmin cm;

    private DateTime initialDeactivationTime;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ApartmentModuleImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ApartmentModule.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        switch (config.modbusUnitId()) {
            case ID_1:
            case ID_4:
            case ID_5:
                this.topAM = false;
                break;
            case ID_2:
            case ID_3:
                this.topAM = true;
                break;
        }
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId().getValue(), this.cm,
                "Modbus", config.modbusBridgeId());
        debugOn = config.debug();
        doOnce = true;
        resetExtReq = config.resetRequestFlag();
        switchRelay1 = OnOff.OFF;
        switchRelay2 = OnOff.OFF;
        relayTime = config.relayTime();
        if (config.turnOnRelay() != 1 && config.turnOnRelay() != 2) {
            throw new ConfigurationException("Activate of ApartmentModule " + super.id(), "Wrong turnOnRelay: Expected 1 or 2, Received: " + config.turnOnRelay());
        }
        this.relaysForOpeningHydraulicMixer = config.turnOnRelay();
        temperatureCalibration = config.tempCal();

    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

        // Select Modbus mapping based on Apartment Module configuration.
        if (topAM) {
            return new ModbusProtocol(this,
                    new FC4ReadInputRegistersTask(0, Priority.HIGH,
                            m(ApartmentModule.ChannelId.IR_0_VERSION, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_1_APARTMENT_MODULE_CONFIGURATION, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_2_ERROR, new UnsignedWordElement(2),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_3_LOOP_TIME, new UnsignedWordElement(3),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_4_EXTERNAL_REQUEST_ACTIVE, new UnsignedWordElement(4),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_5_REQUEST_SIGNAL_TIME, new UnsignedWordElement(5),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_6_TEMPERATURE, new SignedWordElement(6),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(10, Priority.HIGH,
                            m(ApartmentModule.ChannelId.IR_10_STATE_RELAY1, new UnsignedWordElement(10),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_11_RELAY1_REMAINING_TIME, new UnsignedWordElement(11),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(20, Priority.HIGH,
                            m(ApartmentModule.ChannelId.IR_20_STATE_RELAY2, new UnsignedWordElement(20),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_21_RELAY2_REMAINING_TIME, new UnsignedWordElement(21),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),

                    new FC3ReadRegistersTask(0, Priority.HIGH,
                            m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(10, Priority.LOW,
                            m(ApartmentModule.ChannelId.HR_10_COMMAND_RELAY1, new SignedWordElement(10),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_11_TIMING_RELAY1, new SignedWordElement(11),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(20, Priority.LOW,
                            m(ApartmentModule.ChannelId.HR_20_COMMAND_RELAY2, new SignedWordElement(20),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_21_TIMING_RELAY2, new SignedWordElement(21),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),

                    // Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
                    // Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
                    // "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
                    // "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
                    // two separate channels.
                    // That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
                    // watch the order in which you call read and write tasks.
                    // Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
                    // not be transferred to the "value" field of the channel, unless you add code that does that.
                    new FC16WriteRegistersTask(0,
                            m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(10,
                            m(ApartmentModule.ChannelId.HR_10_COMMAND_RELAY1, new UnsignedWordElement(10),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_11_TIMING_RELAY1, new UnsignedWordElement(11),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(20,
                            m(ApartmentModule.ChannelId.HR_20_COMMAND_RELAY2, new UnsignedWordElement(20),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_21_TIMING_RELAY2, new UnsignedWordElement(21),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    )
            );

        } else {
            return new ModbusProtocol(this,
                    new FC4ReadInputRegistersTask(0, Priority.HIGH,
                            m(ApartmentModule.ChannelId.IR_0_VERSION, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_1_APARTMENT_MODULE_CONFIGURATION, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_2_ERROR, new UnsignedWordElement(2),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_3_LOOP_TIME, new UnsignedWordElement(3),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_4_EXTERNAL_REQUEST_ACTIVE, new UnsignedWordElement(4),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.IR_5_REQUEST_SIGNAL_TIME, new UnsignedWordElement(5),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),

                    new FC3ReadRegistersTask(0, Priority.LOW,
                            m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),

                    new FC16WriteRegistersTask(0,
                            m(ApartmentModule.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(ApartmentModule.ChannelId.HR_1_EXTERNAL_REQUEST_FLAG, new UnsignedWordElement(1),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    )
            );
        }
    }

    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                if (debugOn) {
                    channeltest();    // Just for testing
                }
                if (topAM) {
                    if (this.getStateRelay1().equals(OnOff.UNDEFINED) == false && this.getStateRelay2().equals(OnOff.UNDEFINED) == false) {
                        updateValveStatus();
                    } else {
                        this.getValveStatusChannel().setNextValue(ValveStatus.UNDEFINED);
                    }
                    if (this.getTemperatureChannel().value().isDefined()) {
                        this.getLastKnowTemperatureChannel().setNextValue(this.getTemperatureChannel().value().get());
                    }
                }
                if (getSetCommunicationCheckChannel().value().asEnum() != CommunicationCheck.RECEIVED) {
                    if (debugOn) {
                        this.logInfo(this.log, "Sending CommunicationCheck");
                    }
                    try {
                        this.getSetCommunicationCheckChannel().setNextWriteValue(CommunicationCheck.RECEIVED);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        // Wo ist hier n Communicationfail? der schreibt lokal? <-- wenn nix im Value nächstes mal drin steht dann...
                        this.logError(this.log, "Modbus connection to Apartment module failed.");
                    }
                    //TODO Check if still active after x Time
                }
                Optional<Boolean> externalHeatFlag = this.getSetExternalRequestFlagChannel().getNextWriteValueAndReset();
                try {
                    this.getSetExternalRequestFlagChannel().setNextWriteValue(false);
                } catch (OpenemsError.OpenemsNamedException ignored) {
                }
                boolean externalRequestPresent = this.getExternalRequestCurrent().isDefined();
                boolean externalRequest = externalRequestPresent ? this.getExternalRequestCurrent().get() : false;
                if (externalHeatFlag.isPresent() && externalRequestPresent) {
                    this.getLastKnownRequestStatusChannel().setNextValue(externalHeatFlag.orElse(false) || externalRequest);
                }

                this.isHeatRequestFlagChannel().setNextValue(externalHeatFlag.orElse(false));

                // Set temperature calibration
                if (doOnce) {
                    if (topAM) {
                        try {
                            this.setTemperatureCalibrationChannel().setNextWriteValue(temperatureCalibration);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.logError(this.log, "Failed to set temperature calibration value.");
                        }
                    }
                    doOnce = false;

                    if (debugOn) {
                        if (resetExtReq) {
                            try {
                                this.getSetExternalRequestFlagChannel().setNextWriteValue(false);
                            } catch (OpenemsError.OpenemsNamedException e) {
                                this.logError(this.log, "Failed to reset External Request Flag.");
                            }
                        }
                    }
                }
                break;
            case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
                if (topAM) {
                    listenToActivation();
                }

        }
    }

    private void updateValveStatus() {
        boolean relay1 = getStateRelay1().getValue() == 1;
        boolean relay2 = getStateRelay2().getValue() == 1;
        boolean relay1IsActivation = this.relaysForOpeningHydraulicMixer == 1;
        if (relay1 && relay2) {
            this.getValveStatusChannel().setNextValue(ValveStatus.ERROR);
        } else if (relay1) {
            if (relay1IsActivation) {
                this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
            } else {
                this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
            }
        } else if (relay2) {
            if (relay1IsActivation) {
                this.getValveStatusChannel().setNextValue(ValveStatus.CLOSING);
            } else {
                this.getValveStatusChannel().setNextValue(ValveStatus.OPENING);
            }
        } else {
            //both off:
            if (this.isOpen) {
                this.getValveStatusChannel().setNextValue(ValveStatus.OPEN);
            } else if (this.isClosed) {
                this.getValveStatusChannel().setNextValue(ValveStatus.CLOSED);
            }

        }
    }

    private void listenToActivation() {
        Optional<Boolean> activation = this.isActivationRequest().getNextWriteValueAndReset();
        boolean hydraulicMixerActivation = activation.orElse(false);
        if (hydraulicMixerActivation) {
            turnOnHydraulicMixer();
            this.wasDeactivatedBefore = false;
            this.relaysShutDown = false;
        } else {
            if (this.wasDeactivatedBefore == false) {
                turnOffHydraulicMixer();
                this.wasDeactivatedBefore = true;
                this.initialDeactivationTime = new DateTime();
            } else if (this.relaysShutDown == false) {
                shutDownRelays();
            }
        }
    }

    private void shutDownRelays() {
        DateTime now = new DateTime();
        DateTime compare = new DateTime(this.initialDeactivationTime).plusSeconds((DEFAULT_VALVE_MAX_SECONDS));
        if (now.isAfter(compare)) {
            this.setRelay1(OnOff.OFF, DEFAULT_VALVE_PERMANENT);
            this.setRelay2(OnOff.OFF, DEFAULT_VALVE_PERMANENT);
            this.relaysShutDown = true;
        }
    }

    private void turnOffHydraulicMixer() {
        boolean deactivation = this.relaysForOpeningHydraulicMixer == 1;
        this.setRelay1((deactivation ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
        this.setRelay2((deactivation ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
        this.isClosed = true;
        this.isOpen = false;
    }

    private void turnOnHydraulicMixer() {
        boolean activation = this.relaysForOpeningHydraulicMixer == 1;
        this.setRelay1((activation ? OnOff.ON : OnOff.OFF), DEFAULT_VALVE_PERMANENT);
        this.setRelay2((activation ? OnOff.OFF : OnOff.ON), DEFAULT_VALVE_PERMANENT);
        this.isOpen = true;
        this.isClosed = false;
    }

    // Just for testing. Also, example code with some explanations.
    protected void channeltest() {
        if (topAM) {
            this.logInfo(this.log, "--Testing Channels--");
            this.logInfo(this.log, "Input Registers");
            this.logInfo(this.log, "0 Version Number: " + getVersionNumber().get());
            this.logInfo(this.log, "1 Configuration: " + getAmConfiguration().getName()); // Gets the "name" field of the Enum.
            this.logInfo(this.log, "2 Error: " + getError().getName());
            this.logInfo(this.log, "3 Loop Time: " + getLoopTime().get() + " ms");
            this.logInfo(this.log, "4 External Request Active: " + getExternalRequestCurrent().get());
            this.logInfo(this.log, "5 Request Signal Time: " + getRequestSignalTime().get() + " ms");
            this.logInfo(this.log, "6 Temperature: " + getTemperature().orElse(0) / 10.0 + "°C");
            this.logInfo(this.log, "10 State Relay1: " + getStateRelay1().getName());
            this.logInfo(this.log, "11 Relay1 Remaining Time: " + getRelay1RemainingTime().orElse(0) / 100.0 + " s");
            this.logInfo(this.log, "20 State Relay2: " + getStateRelay2().getName());
            this.logInfo(this.log, "21 Relay2 Remaining Time: " + getRelay2RemainingTime().orElse(0) / 100.0 + " s");
            this.logInfo(this.log, "");
            this.logInfo(this.log, "Holding Registers");
            this.logInfo(this.log, "0 Modbus Communication Check: " + getSetCommunicationCheckChannel().value().asEnum().getName()); // Gets the "name" field of the Enum.
            this.logInfo(this.log, "1 External Request Flag: " + getSetExternalRequestFlagChannel().value().get());
            this.logInfo(this.log, "2 Temperature Calibration: " + setTemperatureCalibrationChannel().value().get());
            this.logInfo(this.log, "10 Command for Relay1: " + setCommandRelay1Channel().value().asEnum().getValue());
            this.logInfo(this.log, "11 Timing for Relay1: " + setTimeRelay1Channel().value().get());
            this.logInfo(this.log, "20 Command for Relay2: " + setCommandRelay2Channel().value().asEnum().getValue());
            this.logInfo(this.log, "21 Timing for Relay2: " + setTimeRelay2Channel().value().get());
            this.logInfo(this.log, "");


            // Test Modbus write.
          /*  if (testcounter == 2) {
                this.logInfo(this.log, "Setting Relay1 to " + switchRelay1.getName() + ".");
                this.logInfo(this.log, "Setting Relay2 to " + switchRelay2.getName() + ".");
                this.setRelay1(switchRelay1, relayTime);
                this.setRelay2(switchRelay2, relayTime);
            }

            testcounter++;*/
        } else {
            this.logInfo(this.log, "--Testing Channels--");
            this.logInfo(this.log, "Input Registers");
            this.logInfo(this.log, "0 Version Number: " + getVersionNumber().get());
            this.logInfo(this.log, "1 Configuration: " + getAmConfiguration().getName()); // Gets the "name" field of the Enum.
            this.logInfo(this.log, "2 Error: " + getError().getName());
            this.logInfo(this.log, "3 Loop Time: " + getLoopTime().get() + " ms");
            this.logInfo(this.log, "4 External Request Active: " + getExternalRequestCurrent().get());
            this.logInfo(this.log, "5 Request Signal Time: " + getRequestSignalTime().get() + " ms");
            this.logInfo(this.log, "");
            this.logInfo(this.log, "Holding Registers");
            this.logInfo(this.log, "0 Modbus Communication Check: " + getSetCommunicationCheckChannel().value().asEnum().getName()); // Gets the "name" field of the Enum.
            this.logInfo(this.log, "1 External Request Flag: " + getSetExternalRequestFlagChannel().value().asEnum().getValue());
            this.logInfo(this.log, "");
        }


    }
}
