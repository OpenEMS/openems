package io.openems.edge.evcs.v2.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

@ProviderType
public interface ManagedEvcs extends Evcs {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Gets the smallest power steps that can be set (given in mW).
	 *
	 * <p>
	 * Example:
	 * <ul>
	 * <li>KEBA-series allows setting of milli Ampere. It should return 230 mW
	 * (0.001A * 230V).
	 * <li>Hardy Barth allows setting in Ampere. It should return 230.000 mW (1A *
	 * 230V).
	 * </ul>
	 *
	 * <p>
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Integer
	 * <li>Unit: mW
	 * </ul>
	 */
	POWER_PRECISION(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIWATT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Gets a rough estimation of the time precision the chargepoint has.
	 * 
	 * <p>
	 * It is the time in s between setting charge power and the time the charge
	 * power is applied to the vehicle. Some chargepoints react within milliseconds
	 * and some within 30s. This might allow some controllers to work with cloud
	 * based chargepoints as well.
	 * 
	 * <p>
	 * Note that this is only a very rough estimate. It depends on used connection
	 * and on the response time of vehicles also.
	 * 
	 * <p>
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Read Only
	 * <li>Type: Integer
	 * <li>Unit: Seconds
	 * </ul>
	 */

	TIME_PRECISION(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.SECONDS) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.LOW)),

	/**
	 * Debug readonly Priority of this EVCS.
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>ReadOnly
	 * <li>Type: Priority @see {@link Priority}
	 * </ul>
	 */
	DEBUG_PRIORITY(Doc.of(Priority.values()) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.LOW)),

	/**
	 * Priority of this EVCS.
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Priority @see {@link Priority}
	 * </ul>
	 */
	PRIORITY(Doc.of(Priority.values()) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.LOW)
		.onInit(new EnumWriteChannel.MirrorToDebugChannel(ManagedEvcs.ChannelId.DEBUG_PRIORITY))), //

	/**
	 * Sets the charge power limit of the EVCS in [W].
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	SET_CHARGE_POWER_EQUALS(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
		.accessMode(AccessMode.WRITE_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //
	// TODO when PowerObject is working -> onInit(new PowerConstraint(...)), siehe
	// ManagedSymmetricEss

	/**
	 * Sets the charge power limit of the EVCS in [W].
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	SET_CHARGE_POWER_EQUALS_WITH_FILTER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.accessMode(AccessMode.WRITE_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //
	// TODO when PowerObject is working -> onInit(new PowerConstraint(...)), siehe
	// ManagedSymmetricEss

	/**
	 * Sets the charge power limit of the EVCS in [W].
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	SET_CHARGE_POWER_LESS_OR_EQUALS(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
		.accessMode(AccessMode.WRITE_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //
	// TODO when PowerObject is working -> onInit(new PowerConstraint(...)), siehe
	// ManagedSymmetricEss

	/**
	 * Sets the charge power limit of the EVCS in [W].
	 *
	 * <ul>
	 * <li>Interface: ManagedEvcs
	 * <li>Writable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	SET_CHARGE_POWER_GREATER_OR_EQUALS(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
		.accessMode(AccessMode.WRITE_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //
	// TODO when PowerObject is working -> onInit(new PowerConstraint(...)), siehe
	// ManagedSymmetricEss

	;

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
     * Command to send the given power, to the EVCS.
     * 
     * @param power Power that should be send in watt
     * @return boolean if the power was applied to the EVCS
     * @throws OpenemsException on error
     */
    public boolean applyChargePower(int power) throws Exception;

    /**
     * Get the corresponding {@link EvcsPower} for this EVCS.
     * 
     * @return the {@link EvcsPower}
     */
    public EvcsPower getEvcsPower();

    /**
     * Gets the Channel for {@link ChannelId#POWER_PRECISION}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getPowerPrecisionChannel() {
	return this.channel(ChannelId.POWER_PRECISION);
    }

    /**
     * Gets the power precision value of the EVCS in [W]. See
     * {@link ChannelId#POWER_PRECISION}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getPowerPrecision() {
	return this.getPowerPrecisionChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setPowerPrecision(Integer value) {
	this.getPowerPrecisionChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setPowerPrecision(int value) {
	this.getPowerPrecisionChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#TIME_PRECISION}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getTimePrecisionChannel() {
	return this.channel(ChannelId.TIME_PRECISION);
    }

    /**
     * Gets the time precision value of the EVCS in [ms]. See
     * {@link ChannelId#TIME_PRECISION}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getTimePrecision() {
	return this.getTimePrecisionChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TIME_PRECISION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTimePrecision(Integer value) {
	this.getTimePrecisionChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TIME_PRECISION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTimePrecision(int value) {
	this.getTimePrecisionChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#PRIORITY}.
     *
     * @return the Channel
     */
    public default EnumReadChannel getPriorityChannel() {
	return this.channel(ChannelId.PRIORITY);
    }

    /**
     * Gets the priority of the EVCS. See {@link ChannelId#PRIORITY}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getPriority() {
	return this.getPriorityChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PRIORITY} Channel.
     *
     * @param value the next value
     */
    public default void _setPriority(Integer value) {
	this.getPriorityChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PRIORITY} Channel.
     *
     * @param value the next value
     */
    public default void _setPriority(int value) {
	this.getPriorityChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_EQUALS}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetChargePowerLimitEqualsChannel() {
	return this.channel(ChannelId.SET_CHARGE_POWER_EQUALS);
    }

    /**
     * Gets the set charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetChargePowerLimitEquals() {
	return this.getSetChargePowerLimitEqualsChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitEquals(Integer value) {
	this.getSetChargePowerLimitEqualsChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitEquals(int value) {
	this.getSetChargePowerLimitEqualsChannel().setNextValue(value);
    }

    /**
     * Sets the charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setChargePowerLimitEquals(Integer value) throws OpenemsNamedException {
	this.getSetChargePowerLimitEqualsChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_EQUALS_WITH_FILTER}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetChargePowerLimitEqualsWithFilterChannel() {
	return this.channel(ChannelId.SET_CHARGE_POWER_EQUALS_WITH_FILTER);
    }

    /**
     * Gets the set charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS_WITH_FILTER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetChargePowerLimitEqualsWithFilter() {
	return this.getSetChargePowerLimitEqualsWithFilterChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS_WITH_FILTER} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitEqualsWithFilter(Integer value) {
	this.getSetChargePowerLimitEqualsWithFilterChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS_WITH_FILTER} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitEqualsWithFilter(int value) {
	this.getSetChargePowerLimitEqualsWithFilterChannel().setNextValue(value);
    }

    /**
     * Sets the charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_EQUALS_WITH_FILTER}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setChargePowerLimitEqualsWithFilter(Integer value) throws OpenemsNamedException {
	this.getSetChargePowerLimitEqualsWithFilterChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_LESS_OR_EQUALS}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetChargePowerLimitLessOrEqualsChannel() {
	return this.channel(ChannelId.SET_CHARGE_POWER_LESS_OR_EQUALS);
    }

    /**
     * Gets the set charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_LESS_OR_EQUALS}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetChargePowerLimitLessOrEquals() {
	return this.getSetChargePowerLimitLessOrEqualsChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_LESS_OR_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitLessOrEquals(Integer value) {
	this.getSetChargePowerLimitLessOrEqualsChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_LESS_OR_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitLessOrEquals(int value) {
	this.getSetChargePowerLimitLessOrEqualsChannel().setNextValue(value);
    }

    /**
     * Sets the charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_LESS_OR_EQUALS}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setChargePowerLimitLessOrEquals(Integer value) throws OpenemsNamedException {
	this.getSetChargePowerLimitLessOrEqualsChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_GREATER_OR_EQUALS}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSetChargePowerLimitGreaterOrEqualsChannel() {
	return this.channel(ChannelId.SET_CHARGE_POWER_GREATER_OR_EQUALS);
    }

    /**
     * Gets the set charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_GREATER_OR_EQUALS}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetChargePowerLimitGreaterOrEquals() {
	return this.getSetChargePowerLimitGreaterOrEqualsChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_GREATER_OR_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitGreaterOrEquals(Integer value) {
	this.getSetChargePowerLimitGreaterOrEqualsChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#SET_CHARGE_POWER_GREATER_OR_EQUALS} Channel.
     *
     * @param value the next value
     */
    public default void _setSetChargePowerLimitGreaterOrEquals(int value) {
	this.getSetChargePowerLimitGreaterOrEqualsChannel().setNextValue(value);
    }

    /**
     * Sets the charge power limit of the EVCS in [W]. See
     * {@link ChannelId#SET_CHARGE_POWER_GREATER_OR_EQUALS}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setChargePowerLimitGreaterOrEquals(Integer value) throws OpenemsNamedException {
	this.getSetChargePowerLimitGreaterOrEqualsChannel().setNextWriteValue(value);
    }

    /**
     * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
     * of this Component.
     *
     * @param accessMode filters the Modbus-Records that should be shown
     * @return the {@link ModbusSlaveNatureTable}
     */
    public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
	return ModbusSlaveNatureTable.of(ManagedEvcs.class, accessMode, 100) //
		.channel(0, ChannelId.POWER_PRECISION, ModbusType.UINT16) //
		.channel(1, ChannelId.TIME_PRECISION, ModbusType.UINT16) //
		.channel(2, ChannelId.PRIORITY, ModbusType.UINT16) //
		.channel(3, ChannelId.SET_CHARGE_POWER_EQUALS, ModbusType.UINT16) //
		.channel(4, ChannelId.SET_CHARGE_POWER_EQUALS_WITH_FILTER, ModbusType.UINT16) //
		.channel(5, ChannelId.SET_CHARGE_POWER_LESS_OR_EQUALS, ModbusType.UINT16) //
		.channel(6, ChannelId.SET_CHARGE_POWER_GREATER_OR_EQUALS, ModbusType.UINT16) //
		.build();
    }
}
