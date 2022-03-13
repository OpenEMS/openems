package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

public interface Evcs extends OpenemsComponent {

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
		 * <li>Type: Status
		 * </ul>
		 */
		STATUS(Doc.of(Status.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Charge Power.
		 *
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
		 * Charging Type.
		 *
		 * <p>
		 * Type of charging.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: ChargingType
		 * </ul>
		 */
		CHARGING_TYPE(Doc.of(ChargingType.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Count of phases, the EV is charging with.
		 *
		 * <p>
		 * This value is derived from the charging station or calculated during the
		 * charging.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		PHASES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Minimum Power valid by the hardware.
		 *
		 * <p>
		 * In the cases that the EVCS can't be controlled, the Minimum will be the
		 * maximum too.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MINIMUM_HARDWARE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum Power valid by the hardware.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAXIMUM_HARDWARE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum Power defined by software.
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
		 * Minimum Power defined by software.
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
		CHARGINGSTATION_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
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
	 * Gets the Channel for {@link ChannelId#PHASES}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPhasesChannel() {
		return this.channel(ChannelId.PHASES);
	}

	/**
	 * Gets the Count of phases, the EV is charging with. See
	 * {@link ChannelId#PHASES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPhases() {
		return this.getPhasesChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PHASES} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPhases(Integer value) {
		this.getPhasesChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#PHASES} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPhases(int value) {
		this.getPhasesChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MINIMUM_HARDWARE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinimumHardwarePowerChannel() {
		return this.channel(ChannelId.MINIMUM_HARDWARE_POWER);
	}

	/**
	 * Gets the Minimum Power valid by the hardware in [W]. See
	 * {@link ChannelId#MINIMUM_HARDWARE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinimumHardwarePower() {
		return this.getMinimumHardwarePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MINIMUM_HARDWARE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinimumHardwarePower(Integer value) {
		this.getMinimumHardwarePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MINIMUM_HARDWARE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinimumHardwarePower(int value) {
		this.getMinimumHardwarePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAXIMUM_HARDWARE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaximumHardwarePowerChannel() {
		return this.channel(ChannelId.MAXIMUM_HARDWARE_POWER);
	}

	/**
	 * Gets the Maximum Power valid by the hardware in [W]. See
	 * {@link ChannelId#MAXIMUM_HARDWARE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaximumHardwarePower() {
		return this.getMaximumHardwarePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAXIMUM_HARDWARE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaximumHardwarePower(Integer value) {
		this.getMaximumHardwarePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAXIMUM_HARDWARE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaximumHardwarePower(int value) {
		this.getMaximumHardwarePowerChannel().setNextValue(value);
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
	 * Internal method to set the 'nextValue' on {@link ChannelId#MINIMUM_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinimumPower(int value) {
		this.getMinimumPowerChannel().setNextValue(value);
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
	 * Gets the Channel for {@link ChannelId#CHARGINGSTATION_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getChargingstationCommunicationFailedChannel() {
		return this.channel(ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Failed state channel for a failed communication to the EVCS. See
	 * {@link ChannelId#CHARGINGSTATION_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getChargingstationCommunicationFailed() {
		return this.getChargingstationCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGINGSTATION_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingstationCommunicationFailed(boolean value) {
		this.getChargingstationCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Returns the modbus table for this nature.
	 *
	 * @param accessMode accessMode
	 * @return nature table
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(Evcs.class, accessMode, 100) //
				.channel(0, ChannelId.STATUS, ModbusType.UINT16) //
				.channel(1, ChannelId.CHARGE_POWER, ModbusType.UINT16) //
				.channel(2, ChannelId.CHARGING_TYPE, ModbusType.UINT16) //
				.channel(3, ChannelId.PHASES, ModbusType.UINT16) //
				.channel(4, ChannelId.MAXIMUM_HARDWARE_POWER, ModbusType.UINT16) //
				.channel(5, ChannelId.MINIMUM_HARDWARE_POWER, ModbusType.UINT16) //
				.channel(6, ChannelId.MAXIMUM_POWER, ModbusType.UINT16) //
				.channel(7, ChannelId.ENERGY_SESSION, ModbusType.UINT16) //
				.channel(8, ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, ModbusType.UINT16) //
				.build();
	}
}
