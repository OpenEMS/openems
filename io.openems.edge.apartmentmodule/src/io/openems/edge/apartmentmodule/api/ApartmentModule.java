package io.openems.edge.apartmentmodule.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface ApartmentModule extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Version number.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */

        IR_0_VERSION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Configuration of the Apartment Module. 0 for bottom, 1 for top.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */

        IR_1_APARTMENT_MODULE_CONFIGURATION(Doc.of(AMconfiguration.values()).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error code. Three error bits transmitted as an integer. 0 means no error.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 - 7
         * </ul>
         */

        IR_2_ERROR(Doc.of(Error.values()).accessMode(AccessMode.READ_ONLY)),

        /**
         * Loop time. How long it takes the Apartment Module to execute the main software loop. This is the rate at
         * which the Apartment Module updates it’s values.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: milliseconds
         * </ul>
         */

        IR_3_LOOP_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Is an external request currently active?
         * <ul>
         *      <li> Type: boolean
         * </ul>
         */

        IR_4_EXTERNAL_REQUEST_ACTIVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Duration of the last detected external request in ms. If the request is still active, this counter will
         * continue to go up. If the request is not active anymore, it will keep the value until reset. Will reset when
         * “External request flag” is reset.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: milliseconds
         * </ul>
         */

        IR_5_REQUEST_SIGNAL_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Temperature.
         * <li>
         * <li> Type: Integer
         * <li> Unit: dezidegree celsius
         * </ul>
         */

        IR_6_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * State of relay 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1
         *      <li> State 0: Off
         *      <li> State 1: On
         * </ul>
         */

        IR_10_STATE_RELAY1(Doc.of(OnOff.values()).accessMode(AccessMode.READ_ONLY)),

        /**
         * If relay 1 has been switched on or off with a timer, this is the remaining time before it switches back.
         * Unit is 1/100 s, so 100 is 1 second.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: centiseconds = milliseconds *10
         * </ul>
         */

        IR_11_RELAY1_REMAINING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTISECONDS).accessMode(AccessMode.READ_ONLY)),

        /**
         * State of relay 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1
         *      <li> State 0: Off
         *      <li> State 1: On
         * </ul>
         */

        IR_20_STATE_RELAY2(Doc.of(OnOff.values()).accessMode(AccessMode.READ_ONLY)),

        /**
         * If relay 2 has been switched on or off with a timer, this is the remaining time before it switches back.
         * Unit is 1/100 s, so 100 is 1 second.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: centi seconds = milliseconds *10
         * </ul>
         */

        IR_21_RELAY2_REMAINING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTISECONDS).accessMode(AccessMode.READ_ONLY)),


        // Holding Registers

        /**
         * Modbus communication check. The master should continuously write 1 in this register. If this does not happen
         * for 2 minutes, the Apartment Module will restart. The Apartment Module will reset this to 0 every 5 seconds.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1
         *      <li> State 0: Slave is waiting for signal
         *      <li> State 1: Signal has been set.
         * </ul>
         */

        HR_0_COMMUNICATION_CHECK(Doc.of(CommunicationCheck.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * External request flag. If an external request has been detected. Will stay true until the master resets this
         * to false by writing false in this register.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */

        HR_1_EXTERNAL_REQUEST_FLAG(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Temperature calibration. Value to calibrate the PT1000 sensor.
         * <li>
         * <li> Type: Integer
         * </ul>
         */

        HR_2_TEMPERATURE_CALIBRATION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Command for relay 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1, -1 (undefined)
         *      <li> State 0: Off
         *      <li> State 1: On
         *      <li> State -1: default state, waiting for next command.
         * </ul>
         */

        HR_10_COMMAND_RELAY1(Doc.of(OnOff.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Timing for relay 1. Unit is 1/100 s, so 100 is 1 second.
         * <li>
         * <li>Type: Integer
         * <li>Unit: centi seconds = milliseconds *10
         * </ul>
         */

        HR_11_TIMING_RELAY1(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTISECONDS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Command for relay 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1, -1 (undefined)
         *      <li> State 0: Off
         *      <li> State 1: On
         *      <li> State -1: default state, waiting for next command.
         * </ul>
         */

        HR_20_COMMAND_RELAY2(Doc.of(OnOff.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Timing for relay 2. Unit is 1/100 s, so 100 is 1 second.
         * <li>
         * <li>Type: Integer
         * <li>Unit: centi seconds = milliseconds *10
         * </ul>
         */

        HR_21_TIMING_RELAY2(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTISECONDS).accessMode(AccessMode.READ_WRITE)),

        /**
         * True --> Activates Hydraulic Mixer
         * False --> Deactivates Hydraulic Mixer.
         */
        ACTIVATE_HYDRAULIC_MIXER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * The PowerLevel of the Valve (Depends on Seconds open/Close Time etc)
         * TODO.
         */
        POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        EXTERNAL_HEAT_FLAG(Doc.of(OpenemsType.BOOLEAN)),
        VALVE_STATUS(Doc.of(ValveStatus.values())),
        LAST_KNOWN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        LAST_KNOWN_REQUEST_STATUS(Doc.of(OpenemsType.BOOLEAN));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link ChannelId#IR_0_VERSION}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVersionChannel() {
        return this.channel(ChannelId.IR_0_VERSION);
    }

    /**
     * Gets the version number of the software running on the Apartment module.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVersionNumber() {
        return this.getVersionChannel().value();
    }


    /**
     * Gets the Channel for {@link ChannelId#IR_1_APARTMENT_MODULE_CONFIGURATION}.
     *
     * @return the Channel
     */
    public default Channel<AMconfiguration> getAmConfigurationChannel() {
        return this.channel(ChannelId.IR_1_APARTMENT_MODULE_CONFIGURATION);
    }

    /**
     * Gets the configuration of the Apartment Module.
     *
     * @return the Channel {@link Value}
     */
    public default AMconfiguration getAmConfiguration() {
        return this.getAmConfigurationChannel().value().asEnum();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_2_ERROR}.
     *
     * @return the Channel
     */
    public default Channel<Error> getErrorChannel() {
        return this.channel(ChannelId.IR_2_ERROR);
    }

    /**
     * Returns the error message of the Apartment Module.
     *
     * @return the Channel {@link Value}
     */
    public default Error getError() {
        return this.getErrorChannel().value().asEnum();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_3_LOOP_TIME}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getLoopTimeChannel() {
        return this.channel(ChannelId.IR_3_LOOP_TIME);
    }

    /**
     * Gets the execution time of the main software loop in ms. This is the rate at which the Apartment Module updates
     * it’s values.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getLoopTime() {
        return this.getLoopTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_4_EXTERNAL_REQUEST_ACTIVE}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getExternalRequestCurrentChannel() {
        return this.channel(ChannelId.IR_4_EXTERNAL_REQUEST_ACTIVE);
    }

    /**
     * Returns if the external request is currently active.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getExternalRequestCurrent() {
        return this.getExternalRequestCurrentChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_5_REQUEST_SIGNAL_TIME}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRequestSignalTimeChannel() {
        return this.channel(ChannelId.IR_5_REQUEST_SIGNAL_TIME);
    }

    /**
     * Gets the duration of the last detected external request in ms. Will reset when “External request flag” is reset.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRequestSignalTime() {
        return this.getRequestSignalTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_6_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getTemperatureChannel() {
        return this.channel(ChannelId.IR_6_TEMPERATURE);
    }

    /**
     * Gets the value of the temperature sensor in dezidegree Celsius. (1/10 °C)
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getTemperature() {
        return this.getTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_10_STATE_RELAY1}.
     *
     * @return the Channel
     */
    public default Channel<OnOff> getStateRelay1Channel() {
        return this.channel(ChannelId.IR_10_STATE_RELAY1);
    }

    /**
     * Returns the state of relay 1.
     *
     * @return the Channel {@link Value}
     */
    public default OnOff getStateRelay1() {
        return this.getStateRelay1Channel().value().asEnum();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_11_RELAY1_REMAINING_TIME}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRelay1RemainingTimeChannel() {
        return this.channel(ChannelId.IR_11_RELAY1_REMAINING_TIME);
    }

    /**
     * If relay 1 has been switched on or off with a timer, this is the remaining time before it switches back.
     * Unit is 1/100 s, so 100 is 1 second.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRelay1RemainingTime() {
        return this.getRelay1RemainingTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_20_STATE_RELAY2}.
     *
     * @return the Channel
     */
    public default Channel<OnOff> getStateRelay2Channel() {
        return this.channel(ChannelId.IR_20_STATE_RELAY2);
    }

    /**
     * Returns the state of relay 2.
     *
     * @return the Channel {@link Value}
     */
    public default OnOff getStateRelay2() {
        return this.getStateRelay2Channel().value().asEnum();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_21_RELAY2_REMAINING_TIME}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRelay2RemainingTimeChannel() {
        return this.channel(ChannelId.IR_21_RELAY2_REMAINING_TIME);
    }

    /**
     * If relay 2 has been switched on or off with a timer, this is the remaining time before it switches back.
     * Unit is 1/100 s, so 100 is 1 second.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRelay2RemainingTime() {
        return this.getRelay2RemainingTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_0_COMMUNICATION_CHECK}.
     *
     * @return the Channel
     */
    public default WriteChannel<CommunicationCheck> getSetCommunicationCheckChannel() {
        return this.channel(ChannelId.HR_0_COMMUNICATION_CHECK);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_1_EXTERNAL_REQUEST_FLAG}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSetExternalRequestFlagChannel() {
        return this.channel(ChannelId.HR_1_EXTERNAL_REQUEST_FLAG);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_2_TEMPERATURE_CALIBRATION}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel setTemperatureCalibrationChannel() {
        return this.channel(ChannelId.HR_2_TEMPERATURE_CALIBRATION);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_10_COMMAND_RELAY1}.
     *
     * @return the Channel
     */
    public default WriteChannel<OnOff> setCommandRelay1Channel() {
        return this.channel(ChannelId.HR_10_COMMAND_RELAY1);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_11_TIMING_RELAY1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel setTimeRelay1Channel() {
        return this.channel(ChannelId.HR_11_TIMING_RELAY1);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_20_COMMAND_RELAY2}.
     *
     * @return the Channel
     */
    public default WriteChannel<OnOff> setCommandRelay2Channel() {
        return this.channel(ChannelId.HR_20_COMMAND_RELAY2);
    }

    default Channel<ValveStatus> getValveStatusChannel() {
        return this.channel(ChannelId.VALVE_STATUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR_21_TIMING_RELAY2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel setTimeRelay2Channel() {
        return this.channel(ChannelId.HR_21_TIMING_RELAY2);
    }

    public default boolean setRelay1(OnOff state, int time) {
        try {
            this.setCommandRelay1Channel().setNextWriteValue(state);
            this.setTimeRelay1Channel().setNextWriteValue(time);
        } catch (OpenemsError.OpenemsNamedException e) {
            return false;
        }
        return true;
    }

    public default boolean setRelay2(OnOff state, int time) {
        try {
            this.setCommandRelay2Channel().setNextWriteValue(state);
            this.setTimeRelay2Channel().setNextWriteValue(time);
        } catch (OpenemsError.OpenemsNamedException e) {
            return false;
        }
        return true;
    }

    default WriteChannel<Boolean> isActivationRequest() {
        return this.channel(ChannelId.ACTIVATE_HYDRAULIC_MIXER);
    }

    default boolean isModuleWithRelays() {
        return this.getAmConfiguration().getValue() == 1;
    }

    default boolean hasHeatRequest() {
        Value<Boolean> request = this.getExternalRequestCurrent();
        if (request.isDefined() == false) {
            request = this.getExternalRequestCurrentChannel().getNextValue();
        }
        if (request.isDefined() == false) {
            return false;
        } else {
            return request.get();
        }
    }

    default Channel<Boolean> isHeatRequestFlagChannel() {
        return this.channel(ChannelId.EXTERNAL_HEAT_FLAG);
    }

    default boolean isHeatRequestFlag() {
        return this.isHeatRequestFlagChannel().value().isDefined() ? this.isHeatRequestFlagChannel().value().get() : false;
    }

    default Channel<Integer> getLastKnowTemperatureChannel() {
        return this.channel(ChannelId.LAST_KNOWN_TEMPERATURE);
    }

    default Channel<Boolean> getLastKnownRequestStatusChannel(){
        return this.channel(ChannelId.LAST_KNOWN_REQUEST_STATUS);
    }

}
