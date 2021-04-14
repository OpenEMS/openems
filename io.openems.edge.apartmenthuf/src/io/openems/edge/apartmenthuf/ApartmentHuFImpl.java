package io.openems.edge.apartmenthuf;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.*;
import io.openems.edge.bridge.modbus.api.task.*;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.apartmenthuf.api.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "ApartmentHuF",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

/**
 * This module reads all variables available via Modbus from a Consolinno Apartment HuF and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the Apartment HuF via "setNextWriteValue" method.
 */

public class ApartmentHuFImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler, ApartmentHuFChannel {

    @Reference
    protected ConfigurationAdmin cm;


    private final Logger log = LoggerFactory.getLogger(ApartmentHuFImpl.class);

    private int temperatureCalibration;
    private boolean debugOn;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ApartmentHuFImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ApartmentHuFChannel.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        debugOn = config.debug();
        temperatureCalibration = config.tempCal();

    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {

        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(0, Priority.HIGH,
                        m(ApartmentHuFChannel.ChannelId.IR_0_VERSION, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuFChannel.ChannelId.IR_2_ERROR, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuFChannel.ChannelId.IR_3_LOOP_TIME, new UnsignedWordElement(3),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(4),
                        new DummyRegisterElement(5),
                        m(ApartmentHuFChannel.ChannelId.IR_6_WALL_TEMPERATURE_HUF, new SignedWordElement(6),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuFChannel.ChannelId.IR_7_AIR_TEMPERATURE_HUF, new SignedWordElement(7),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ApartmentHuFChannel.ChannelId.IR_8_AIR_HUMIDITY_HUF, new SignedWordElement(8),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(ApartmentHuFChannel.ChannelId.IR_9_AIR_PRESSURE_HUF, new SignedWordElement(9),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
                ),

                new FC3ReadRegistersTask(0, Priority.HIGH,
                        m(ApartmentHuFChannel.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuFChannel.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
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
                        m(ApartmentHuFChannel.ChannelId.HR_0_COMMUNICATION_CHECK, new UnsignedWordElement(0),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(1),
                        m(ApartmentHuFChannel.ChannelId.HR_2_TEMPERATURE_CALIBRATION, new UnsignedWordElement(2),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

    }

    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {

            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                //since modbusCommunication can send null values/at least get last value
                updateHufChannelForInternalUse();
                break;


            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                if (debugOn) {
                    channeltest();    // Just for testing
                }
                if (getSetCommunicationCheckChannel().value().asEnum() != CommunicationCheck.RECEIVED) {
                    if (debugOn) {
                        this.logInfo(this.log, "Sending CommunicationCheck.");
                    }
                    try {
                        this.getSetCommunicationCheckChannel().setNextWriteValue(CommunicationCheck.RECEIVED);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logError(this.log, "Modbus connection to Apartment module failed.");
                    }
                }

                // Set temperature calibration
                if (setTemperatureCalibrationChannel().value().orElse(0) != temperatureCalibration) {
                    if (debugOn) {
                        this.logInfo(this.log, "Setting PT1000 calibration value.");
                    }
                    try {
                        this.setTemperatureCalibrationChannel().setNextWriteValue(temperatureCalibration);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.logError(this.log, "Failed to set PT1000 calibration value.");
                    }
                }
                break;
        }
    }

    private void updateHufChannelForInternalUse() {
        if (this.getWallTemperatureToHufChannel().value().isDefined()) {
            this.getWallTemperatureChannel().setNextValue(this.getWallTemperatureToHufChannel().value().get());
        }
        if (this.getAirTemperatureToHufChannel().value().isDefined()) {
            this.getAirTemperatureChannel().setNextValue(this.getAirTemperatureToHufChannel().value().get());
        }
        if (this.getAirHumidityToHufChannel().value().isDefined()) {
            this.getAirHumidityChannel().setNextValue(this.getAirHumidityToHufChannel().value().get());
        }
        if (this.getAirPressureToHufChannel().value().isDefined()) {
            this.getAirPressureChannel().setNextValue(this.getAirPressureToHufChannel().value().get());
        }
    }

    // Just for testing. Also, example code with some explanations.
    protected void channeltest() {
        this.logInfo(this.log, "--Testing Channels--");
        this.logInfo(this.log, "Input Registers");
        this.logInfo(this.log, "0 Version Number: " + getVersionNumber().get());
        this.logInfo(this.log, "2 Error: " + getError().getName());
        this.logInfo(this.log, "3 Loop Time: " + getLoopTime().get() + " ms");
        this.logInfo(this.log, "6 Wall Temperature: " + getWallTemperature().orElse(0) / 10.0 + "°C");
        this.logInfo(this.log, "7 Air Temperature: " + getAirTemperature().orElse(0) / 10.0 + "°C");
        this.logInfo(this.log, "8 Air Humidity: " + getAirHumidity().orElse(0.0f) + "%");
        this.logInfo(this.log, "9 Air Pressure: " + getAirPressure().orElse(0.0f) + "hPa");
        this.logInfo(this.log, "");
        this.logInfo(this.log, "Holding Registers");
        this.logInfo(this.log, "0 Modbus Communication Check: " + getSetCommunicationCheckChannel().value().asEnum().getName()); // Gets the "name" field of the Enum.
        this.logInfo(this.log, "2 Temperature Calibration: " + setTemperatureCalibrationChannel().value().get());
        this.logInfo(this.log, "");
    }

}
