package io.openems.edge.evcs.v2.api;

import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

public interface Evcs extends OpenemsComponent {

    public static final Integer DEFAULT_MAXIMUM_HARDWARE_POWER = 22_080; // W
    public static final Integer DEFAULT_MINIMUM_HARDWARE_POWER = 4_140; // W
    public static final Integer DEFAULT_MAXIMUM_HARDWARE_CURRENT = 32_000; // mA
    public static final Integer DEFAULT_MINIMUM_HARDWARE_CURRENT = 6_000; // mA
    public static final Integer DEFAULT_VOLTAGE = 230; // V
    public static final int DEFAULT_POWER_RECISION = 230;

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Status.
	 *
	 * <p>
	 * The Status of the EVCS charging station.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Status @see {@link Status}
	 * </ul>
	 */
	STATUS(Doc.of(Status.values()) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Charge Power.
	 *
	 * <p>
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Minimum Power this hardware needs to run.
	 * 
	 * <p>
	 * The power is always given for all three phases. If MinimumPower = 4140W
	 * (3*1380W) and a vehicle charges on 2 phases than the MinimumPower the EVCS
	 * uses is 2706W (2*1380W).
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	MINIMUM_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Maximum Power defined by software.
	 *
	 * <p>
	 * The power is always given for all three phases. If MaximumPower = 22080W
	 * (3*73600W) and a vehicle charges on 2 phases than the MaximumPower for this
	 * EVCS is 14720W (2*7360W).
	 * 
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: W
	 * </ul>
	 */
	MAXIMUM_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Charging Type.
	 *
	 * <p>
	 * Type of charging.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: ChargingType @see {@link ChargingType}
	 * </ul>
	 */
	CHARGING_TYPE(Doc.of(ChargingType.values()) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Current.
	 *
	 * <p>
	 * The current for all three phases in mA.
	 * 
	 * <p>
	 * See helper Evcs.initializeCurrentChannel() also.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: Milliampere
	 * </ul>
	 */
	CURRENT(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)),

	/**
	 * CurrentL1.
	 *
	 * <p>
	 * The current for phase L1.
	 * 
	 * <p>
	 * In charge parks the phases of chargepoints are rotated when installed. Thus a
	 * chargepoint measures current on L1 but it may actually be connected to L2 or
	 * L3.
	 * 
	 * <p>
	 * CurrentL1 needs to reflect the real phase L1, thus the driver is responsible
	 * to rotating the phases before updating this channel. Thus the controller
	 * using this channel can always trust that CURRENT_L1 reflects the current on
	 * Phase 1.
	 * 
	 * <p>
	 * chargepoint driver developer may use the following helper method:
	 * this.getPhaseRotation().getFirstPhase()
	 * 
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: Milliampere
	 * </ul>
	 */
	CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)),

	/**
	 * CurrentL2.
	 *
	 * <p>
	 * The current for phase L2.
	 * 
	 * <p>
	 * In charge parks the phases of chargepoints are rotated when installed. Thus a
	 * chargepoint measures current on L2 but it may actually be connected to L2 or
	 * L3.
	 * 
	 * <p>
	 * CurrentL2 needs to reflect the real phase L2, thus the driver is responsible
	 * to rotating the phases before updating this channel. Thus the controller
	 * using this channel can always trust that CURRENT_L2 reflects the current on
	 * Phase 1.
	 * 
	 * <p>
	 * chargepoint driver developer may use the following helper method:
	 * this.getPhaseRotation().getFirstPhase()
	 * 
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: Milliampere
	 * </ul>
	 */
	CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)),

	/**
	 * CurrentL3.
	 *
	 * <p>
	 * The current for phase L3.
	 * 
	 * <p>
	 * In charge parks the phases of chargepoints are rotated when installed. Thus a
	 * chargepoint measures current on L3 but it may actually be connected to L3 or
	 * L3.
	 * 
	 * <p>
	 * CurrentL3 needs to reflect the real phase L3, thus the driver is responsible
	 * to rotating the phases before updating this channel. Thus the controller
	 * using this channel can always trust that CURRENT_L3 reflects the current on
	 * Phase 1.
	 * 
	 * <p>
	 * chargepoint driver developer may use the following helper method:
	 * this.getPhaseRotation().getFirstPhase()
	 * 
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: Milliampere
	 * </ul>
	 */
	CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)),

	/**
	 * Count of phases, the EV is charging with.
	 *
	 * <p>
	 * This value is derived from the charging station or calculated during the
	 * charging. When this value is set, the minimum and maximum limits are set at
	 * the same time if the EVCS is a {@link ManagedEvcs}.
	 * 
	 * <p>
	 * phases must be set to 3 if undefined or unclear.
	 * 
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Phases @see @link {@link Phases}
	 * </ul>
	 */
	PHASES(Doc.of(Phases.values()) //
		.debounce(5) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Phase Rotation.
	 * 
	 * <p>
	 * In charge parks the phases of chargepoints are rotated when installed. This
	 * reduces phase shifting load when the majority of vehicles charge with only
	 * 2phase or less. Channel provides information on the phase rotation of this
	 * chargepoint.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: PhaseRotation @see {@link PhaseRotation}
	 * </ul>
	 */
	PHASE_ROTATION(Doc.of(PhaseRotation.values()) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Energy that was charged during the current or last Session.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Type: Integer
	 * <li>Unit: Wh
	 * </ul>
	 */
	ENERGY_SESSION(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT_HOURS) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Active Consumption Energy.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Type: Integer
	 * <li>Unit: Wh
	 * </ul>
	 */
	ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
		.unit(Unit.WATT_HOURS) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)), //

	/**
	 * Failed state channel for a failed communication to the EVCS.
	 *
	 * <ul>
	 * <li>Interface: Evcs
	 * <li>Readable
	 * <li>Level: FAULT
	 * </ul>
	 */
	COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
		.accessMode(AccessMode.READ_ONLY) //
		.persistencePriority(PersistencePriority.HIGH)); //

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
     * Initializes Channel listeners to set a generic current, which is maxOf(L1,
     * L2, L3) and put the value to the current channel.
     *
     * @param evcs the {@link Evcs}
     */
    public static void initializeCurrentChannel(Evcs evcs) {
	final Consumer<Value<Integer>> currentMax = ignore -> {
	    evcs._setCurrent(TypeUtils.max(//
		    evcs.getCurrentL1Channel().getNextValue().get(), //
		    evcs.getCurrentL2Channel().getNextValue().get(), //
		    evcs.getCurrentL3Channel().getNextValue().get())); //
	};
	evcs.getCurrentL1Channel().onSetNextValue(currentMax);
	evcs.getCurrentL2Channel().onSetNextValue(currentMax);
	evcs.getCurrentL3Channel().onSetNextValue(currentMax);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS}.
     *
     * @return the Channel
     */
    public default Channel<Status> getStatusChannel() {
	return this.channel(ChannelId.STATUS);
    }

    /**
     * Gets the Status of the EVCS charging station. See {@link ChannelId#STATUS}.
     *
     * @return the Channel {@link Value}
     */
    public default Status getStatus() {
	return this.getStatusChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#STATUS} Channel.
     *
     * @param value the next value
     */
    public default void _setStatus(Status value) {
	this.getStatusChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CHARGE_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getChargePowerChannel() {
	return this.channel(ChannelId.CHARGE_POWER);
    }

    /**
     * Gets the Charge Power in [W]. See {@link ChannelId#CHARGE_POWER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getChargePower() {
	return this.getChargePowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHARGE_POWER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setChargePower(Integer value) {
	this.getChargePowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHARGE_POWER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setChargePower(int value) {
	this.getChargePowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MAXIMUM_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMaximumPowerChannel() {
	return this.channel(ChannelId.MAXIMUM_POWER);
    }

    /**
     * Gets the Maximum Power valid by software in [W]. See
     * {@link ChannelId#MAXIMUM_POWER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMaximumPower() {
	return this.getMaximumPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#MAXIMUM_POWER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setMaximumPower(Integer value) {
	this.getMaximumPowerChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#MAXIMUM_POWER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setMaximumPower(int value) {
	this.getMaximumPowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MINIMUM_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMinimumPowerChannel() {
	return this.channel(ChannelId.MINIMUM_POWER);
    }

    /**
     * Gets the Minimum Power valid by software in [W]. See
     * {@link ChannelId#MINIMUM_POWER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMinimumPower() {
	return this.getMinimumPowerChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#MINIMUM_POWER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setMinimumPower(Integer value) {
	this.getMinimumPowerChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CHARGING_TYPE}.
     *
     * @return the Channel
     */
    public default Channel<ChargingType> getChargingTypeChannel() {
	return this.channel(ChannelId.CHARGING_TYPE);
    }

    /**
     * Gets the Type of charging. See {@link ChannelId#CHARGING_TYPE}.
     *
     * @return the Channel {@link Value}
     */
    public default ChargingType getChargingType() {
	return this.getChargingTypeChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_TYPE}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setChargingType(ChargingType value) {
	this.getChargingTypeChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCurrentChannel() {
	return this.channel(ChannelId.CURRENT);
    }

    /**
     * Gets the Current. See {@link ChannelId#MINIMUM_POWER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCurrent() {
	return this.getCurrentChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT} Channel.
     *
     * @param value the next value
     */
    public default void _setCurrent(Integer value) {
	this.getCurrentChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCurrentL1Channel() {
	return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Current. See {@link ChannelId#CURRENT_L1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCurrentL1() {
	return this.getCurrentL1Channel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L1}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setCurrentL1(Integer value) {
	this.getCurrentL1Channel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCurrentL2Channel() {
	return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Current. See {@link ChannelId#CURRENT_L2}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCurrentL2() {
	return this.getCurrentL2Channel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L3}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setCurrentL2(Integer value) {
	this.getCurrentL2Channel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCurrentL3Channel() {
	return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Current. See {@link ChannelId#CURRENT_L3}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCurrentL3() {
	return this.getCurrentL3Channel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L3}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setCurrentL3(Integer value) {
	this.getCurrentL3Channel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#PHASES}.
     *
     * @return the Channel
     */
    public default EnumReadChannel getPhasesChannel() {
	return this.channel(ChannelId.PHASES);
    }

    /**
     * Gets the current Phases definition. See {@link ChannelId#PHASES}.
     *
     * @return the Channel {@link Value}
     */
    public default Phases getPhases() {
	return this.getPhasesChannel().value().asEnum();
    }

    /**
     * Gets the Count of phases, the EV is charging with. See
     * {@link ChannelId#PHASES}.
     *
     * @return the Channel {@link Value}
     */
    public default int getPhasesAsInt() {
	return this.getPhasesChannel().value().asEnum().getValue();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PHASES} Channel.
     *
     * @param value the next value
     */
    public default void _setPhases(Phases value) {
	this.getPhasesChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PHASES} Channel.
     *
     * @param value the next value
     */
    public default void _setPhases(Integer value) {
	if (value == null) {
	    this._setPhases(Phases.THREE_PHASE);
	    return;
	}
	switch (value) {
	case 1:
	    this._setPhases(Phases.ONE_PHASE);
	    break;
	case 2:
	    this._setPhases(Phases.TWO_PHASE);
	    break;
	case 3:
	    this._setPhases(Phases.THREE_PHASE);
	    break;
	default:
	    throw new IllegalArgumentException("Value [" + value + "] for _setPhases is invalid");
	}
    }

    /**
     * Gets the Channel for {@link ChannelId#PHASE_ROTATION}.
     *
     * @return the Channel
     */
    public default EnumReadChannel getPhaseRotationChannel() {
	return this.channel(ChannelId.PHASE_ROTATION);
    }

    /**
     * Gets the current PhaseRotation. See {@link ChannelId#PHASE_ROTATION}.
     *
     * @return the Channel {@link Value}
     */
    public default Phases getPhaseRotation() {
	return this.getPhaseRotationChannel().value().asEnum();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PHASES} Channel.
     *
     * @param value the next value
     */
    public default void _setPhaseRotation(PhaseRotation value) {
	this.getPhaseRotationChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ENERGY_SESSION}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getEnergySessionChannel() {
	return this.channel(ChannelId.ENERGY_SESSION);
    }

    /**
     * Gets the Energy that was charged during the current or last Session in [Wh].
     * See {@link ChannelId#ENERGY_SESSION}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEnergySession() {
	return this.getEnergySessionChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#ENERGY_SESSION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setEnergySession(Integer value) {
	this.getEnergySessionChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#ENERGY_SESSION}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setEnergySession(int value) {
	this.getEnergySessionChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
     *
     * @return the Channel
     */
    public default LongReadChannel getActiveConsumptionEnergyChannel() {
	return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
    }

    /**
     * Gets the Active Consumption Energy in [Wh]. This relates to negative
     * ACTIVE_POWER. See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Long> getActiveConsumptionEnergy() {
	return this.getActiveConsumptionEnergyChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY} Channel.
     *
     * @param value the next value
     */
    public default void _setActiveConsumptionEnergy(Long value) {
	this.getActiveConsumptionEnergyChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY} Channel.
     *
     * @param value the next value
     */
    public default void _setActiveConsumptionEnergy(long value) {
	this.getActiveConsumptionEnergyChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#COMMUNICATION_FAILED}.
     *
     * @return the Channel
     */
    public default StateChannel getCommunicationFailedChannel() {
	return this.channel(ChannelId.COMMUNICATION_FAILED);
    }

    /**
     * Gets the Failed state channel for a failed communication to the EVCS. See
     * {@link ChannelId#COMMUNICATION_FAILED}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getCommunicationFailed() {
	return this.getCommunicationFailedChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on
     * {@link ChannelId#COMMUNICATION_FAILED} Channel.
     *
     * @param value the next value
     */
    public default void _setCommunicationFailed(boolean value) {
	this.getCommunicationFailedChannel().setNextValue(value);
    }

    /**
     * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
     * of this Component.
     *
     * @param accessMode filters the Modbus-Records that should be shown
     * @return the {@link ModbusSlaveNatureTable}
     */
    public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
	return ModbusSlaveNatureTable.of(Evcs.class, accessMode, 100) //
		.channel(0, ChannelId.STATUS, ModbusType.UINT16) //
		.channel(1, ChannelId.CHARGE_POWER, ModbusType.UINT16) //
		.channel(2, ChannelId.CHARGING_TYPE, ModbusType.UINT16) //
		.channel(3, ChannelId.PHASES, ModbusType.UINT16) //
		.channel(4, ChannelId.MINIMUM_POWER, ModbusType.UINT16) //
		.channel(5, ChannelId.MAXIMUM_POWER, ModbusType.UINT16) //
		.channel(6, ChannelId.ENERGY_SESSION, ModbusType.UINT16) //
		.channel(7, ChannelId.COMMUNICATION_FAILED, ModbusType.UINT16) //
		.channel(8, ChannelId.ACTIVE_CONSUMPTION_ENERGY, ModbusType.UINT16) //
		.channel(9, ChannelId.CURRENT, ModbusType.UINT16) //
		.channel(10, ChannelId.CURRENT_L1, ModbusType.UINT16) //
		.channel(11, ChannelId.CURRENT_L2, ModbusType.UINT16) //
		.channel(12, ChannelId.CURRENT_L3, ModbusType.UINT16) //
		.channel(13, ChannelId.PHASE_ROTATION, ModbusType.UINT16) //
		.build();
    }
}
