package io.openems.edge.heater.chp.kwenergy.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.Heater;


public interface ChpKwEnergySmartblock extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
        // Unsigned 16 bit, unless stated otherwise.

        // This CHP maps all values to holding registers no matter if they are read/write or read only. The following
        // registers are read only values.

        /**
         * Error bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR0_ERROR_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR16_STATUS_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status bits 65 - 80.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR20_STATUS_BITS_65_to_80(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Engine temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        HR24_ENGINE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        // HR25_FLOW_TEMPERATURE -> Heater, FLOW_TEMPERATURE, d°C. Value from CHP is same unit.

        // HR26_RETURN_TEMPERATURE -> Heater, RETURN_TEMPERATURE, d°C. Value from CHP is same unit.

        /**
         * Engine rotations per minute. Watch the conversion, the value coming from the device is rpm*10!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: RPM
         * </ul>
         */
        HR31_ENGINE_RPM(Doc.of(OpenemsType.INTEGER).unit(Unit.ROTATION_PER_MINUTE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Effective electric power. Watch the conversion, the value coming from the device is kW*10!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        HR34_EFFECTIVE_ELECTRIC_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * No info available on what the content of this register means.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR81_OPERATING_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Handshake counter out.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR108_HANDSHAKE_OUT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),


        // This CHP maps all values to holding registers no matter if they are read/write or read only. The following
        // registers are read/write values.

        /**
         * Command bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR109_COMMAND_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        // HR111_SET_POINT_POWER_PERCENT -> Heater, SET_POINT_POWER_PERCENT, percent. Value from CHP is percent*10, watch the conversion!

        /**
         * Set point electric power (Netzbezugswert). Watch the conversion, the value in the device is kW*10!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        HR112_SET_POINT_ELECTRIC_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Handshake counter in.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR119_HANDSHAKE_IN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Status of the CHP.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Set the control mode of the CHP.
         * true - use HR112_SET_POINT_ELECTRIC_POWER
         * false - use SET_POINT_POWER_PERCENT (default state)
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        CONTROL_MODE_ELECTRIC_POWER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link ChannelId#HR0_ERROR_BITS_1_to_16}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorBits1to16Channel() {
        return this.channel(ChannelId.HR0_ERROR_BITS_1_to_16);
    }

    /**
     * Gets the error bits 1 - 16.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorBits1to16() { return this.getErrorBits1to16Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR16_STATUS_BITS_1_to_16}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusBits1to16Channel() {
        return this.channel(ChannelId.HR16_STATUS_BITS_1_to_16);
    }

    /**
     * Gets the status bits 1 - 16.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusBits1to16() { return this.getStatusBits1to16Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR20_STATUS_BITS_65_to_80}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusBits65to80Channel() {
        return this.channel(ChannelId.HR20_STATUS_BITS_65_to_80);
    }

    /**
     * Gets the status bits 65 - 80.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusBits65to80() { return this.getStatusBits65to80Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR24_ENGINE_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEngineTemperatureChannel() {
        return this.channel(ChannelId.HR24_ENGINE_TEMPERATURE);
    }

    /**
     * Gets the engine temperature.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEngineTemperature() { return this.getEngineTemperatureChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR31_ENGINE_RPM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEngineRpmChannel() {
        return this.channel(ChannelId.HR31_ENGINE_RPM);
    }

    /**
     * Gets the engine RPM.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEngineRpm() { return this.getEngineRpmChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR34_EFFECTIVE_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEffectiveElectricPowerChannel() {
        return this.channel(ChannelId.HR34_EFFECTIVE_ELECTRIC_POWER);
    }

    /**
     * Gets the effective electric power.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEffectiveElectricPower() { return this.getEffectiveElectricPowerChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR81_OPERATING_MODE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getOperatingModeChannel() {
        return this.channel(ChannelId.HR81_OPERATING_MODE);
    }

    /**
     * Gets the operating mode. (No info so far on what that means)
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOperatingMode() { return this.getOperatingModeChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR108_HANDSHAKE_OUT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHandshakeOutChannel() {
        return this.channel(ChannelId.HR108_HANDSHAKE_OUT);
    }

    /**
     * Gets the handshake counter coming from the device.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHandshakeOut() { return this.getHandshakeOutChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HR109_COMMAND_BITS_1_to_16}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getCommandBits1to16Channel() {
        return this.channel(ChannelId.HR109_COMMAND_BITS_1_to_16);
    }

    /**
     * Gets the command bits 1 - 16.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCommandBits1to16() { return this.getCommandBits1to16Channel().value(); }

    /**
     * Sets the command bits 1 - 16.
     */
    public default void setCommandBits1to16(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBits1to16Channel().setNextWriteValue(value);
    }

    /**
     * Sets the command bits 1 - 16.
     */
    public default void setCommandBits1to16(int value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBits1to16Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR112_SET_POINT_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetPointElectricPowerChannel() {
        return this.channel(ChannelId.HR112_SET_POINT_ELECTRIC_POWER);
    }

    /**
     * Gets the electric power set point.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetPointElectricPower() { return this.getSetPointElectricPowerChannel().value(); }

    /**
     * Sets electric power set point.
     */
    public default void setSetPointElectricPower(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getSetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Sets electric power set point.
     */
    public default void setSetPointElectricPower(int value) throws OpenemsError.OpenemsNamedException {
        this.getSetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR119_HANDSHAKE_IN}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHandshakeInChannel() {
        return this.channel(ChannelId.HR119_HANDSHAKE_IN);
    }

    /**
     * Gets the handshake counter going to the device.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHandshakeIn() { return this.getHandshakeInChannel().value(); }

    /**
     * Sets the handshake counter going to the device.
     */
    public default void setHandshakeIn(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHandshakeInChannel().setNextWriteValue(value);
    }

    /**
     * Sets the handshake counter going to the device.
     */
    public default void setHandshakeIn(int value) throws OpenemsError.OpenemsNamedException {
        this.getHandshakeInChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    public default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Gets the status message.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getStatusMessage() { return this.getStatusMessageChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setStatusMessage(String value) { this.getStatusMessageChannel().setNextValue(value); }

    /**
     * Gets the Channel for {@link ChannelId#CONTROL_MODE_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getControlModeElectricPowerChannel() {
        return this.channel(ChannelId.CONTROL_MODE_ELECTRIC_POWER);
    }

    /**
     * Gets the control mode of the CHP.
     * true - use setSetPointElectricPower()
     * false - use setSetPointPowerPercent() (default state)
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getControlModeElectricPower() { return this.getControlModeElectricPowerChannel().value(); }

    /**
     * Sets the control mode of the CHP.
     * true - use setSetPointElectricPower()
     * false - use setSetPointPowerPercent() (default state)
     */
    public default void setControlModeElectricPower(boolean value) throws OpenemsError.OpenemsNamedException {
        this.getControlModeElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Sets the control mode of the CHP.
     * true - use setSetPointElectricPower()
     * false - use setSetPointPowerPercent() (default state)
     */
    public default void setControlModeElectricPower(Boolean value) throws OpenemsError.OpenemsNamedException {
        this.getControlModeElectricPowerChannel().setNextWriteValue(value);
    }
}
