package io.openems.edge.kostal.plenticore.gridmeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Interface representing the Kostal Grid Meter component.
 *
 * <p>Provides channels for measuring electrical properties such as power, voltage,
 * current, frequency, and energy consumption/production.</p>
 */
public interface KostalGridMeter
        extends ElectricityMeter, ModbusComponent, OpenemsComponent {

    /**
     * Enum for channel identifiers used in the Kostal Grid Meter.
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SCALE_FACTOR_CURRENT(Doc.of(OpenemsType.INTEGER)),
        SCALE_FACTOR_VOLTAGE(Doc.of(OpenemsType.INTEGER)),
        SCALE_FACTOR_POWER(Doc.of(OpenemsType.INTEGER)),
        SCALE_FACTOR_ENERGY(Doc.of(OpenemsType.INTEGER)),
        SCALE_FACTOR_FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ)),
        REAL_IMPORTED_ENERGY(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        REAL_EXPORTED_ENERGY(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        ACTIVE_CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_CONSUMPTION_REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ACTIVE_PRODUCTION_REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Retrieves the scale factor for current measurements.
     *
     * @return the read channel for SCALE_FACTOR_CURRENT.
     */
    public default IntegerReadChannel getScaleFactorCurrentChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_CURRENT);
    }

    /**
     * Gets the current scale factor value.
     *
     * @return the integer value of the scale factor.
     */
    public default Integer getScaleFactorCurrentValue() {
        return this.getScaleFactorCurrentChannel().value().get();
    }

    /**
     * Sets the scale factor for current measurements.
     *
     * @param value the new scale factor value to set.
     */
    public default void _setScaleFactorCurrent(Integer value) {
        this.getScaleFactorCurrentChannel().setNextValue(value);
    }

    /**
     * Retrieves the scale factor for voltage measurements.
     *
     * @return the read channel for SCALE_FACTOR_VOLTAGE.
     */
    public default IntegerReadChannel getScaleFactorVoltageChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_VOLTAGE);
    }

    /**
     * Gets the voltage scale factor value.
     *
     * @return the integer value of the scale factor.
     */
    public default Integer getScaleFactorVoltageValue() {
        return this.getScaleFactorVoltageChannel().value().get();
    }

    /**
     * Sets the scale factor for voltage measurements.
     *
     * @param value the new scale factor value to set.
     */
    public default void _setScaleFactorVoltage(Integer value) {
        this.getScaleFactorVoltageChannel().setNextValue(value);
    }

    /**
     * Retrieves the scale factor for power measurements.
     *
     * @return the read channel for SCALE_FACTOR_POWER.
     */
    public default IntegerReadChannel getScaleFactorPowerChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_POWER);
    }

    /**
     * Gets the power scale factor value.
     *
     * @return the integer value of the scale factor.
     */
    public default Integer getScaleFactorPowerValue() {
        return this.getScaleFactorPowerChannel().value().get();
    }

    /**
     * Sets the scale factor for power measurements.
     *
     * @param value the new scale factor value to set.
     */
    public default void _setScaleFactorPower(Integer value) {
        this.getScaleFactorPowerChannel().setNextValue(value);
    }

    /**
     * Retrieves the scale factor for energy measurements.
     *
     * @return the read channel for SCALE_FACTOR_ENERGY.
     */
    public default IntegerReadChannel getScaleFactorEnergyChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_ENERGY);
    }

    /**
     * Gets the energy scale factor value.
     *
     * @return the integer value of the scale factor.
     */
    public default Integer getScaleFactorEnergyValue() {
        return this.getScaleFactorEnergyChannel().value().get();
    }

    /**
     * Sets the scale factor for energy measurements.
     *
     * @param value the new scale factor value to set.
     */
    public default void _setScaleFactorEnergy(Integer value) {
        this.getScaleFactorEnergyChannel().setNextValue(value);
    }

    /**
     * Retrieves the scale factor for frequency measurements.
     *
     * @return the read channel for SCALE_FACTOR_FREQUENCY.
     */
    public default IntegerReadChannel getScaleFactorFrequencyChannel() {
        return this.channel(ChannelId.SCALE_FACTOR_FREQUENCY);
    }

    /**
     * Gets the frequency scale factor value.
     *
     * @return the integer value of the scale factor.
     */
    public default Integer getScaleFactorFrequencyValue() {
        return this.getScaleFactorFrequencyChannel().value().get();
    }

    /**
     * Sets the scale factor for frequency measurements.
     *
     * @param value the new scale factor value to set.
     */
    public default void _setScaleFactorFrequency(Integer value) {
        this.getScaleFactorFrequencyChannel().setNextValue(value);
    }
}