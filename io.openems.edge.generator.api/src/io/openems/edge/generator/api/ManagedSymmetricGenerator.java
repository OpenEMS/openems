package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;



/**
 * Represents a 3-Phase, symmetric PV-Inverter.
 */
public interface ManagedSymmetricGenerator extends OpenemsComponent, SymmetricGenerator  {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		
		/**
		 * Holds the maximum possible apparent power. This value is defined by the
		 * inverter limitations.
		 *
		 * <ul>
		 * <li>Interface: SymmetricPvInverter
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		GENRATOR_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Holds the maximum possible apparent power. This value is defined by the
		 * inverter limitations.
		 *
		 * <ul>
		 * <li>Interface: SymmetricPvInverter
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		GENRATOR_MAX_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Holds the maximum possible apparent power. This value is defined by the
		 * inverter limitations.
		 *
		 * <ul>
		 * <li>Interface: SymmetricPvInverter
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		GENRATOR_MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Read/Set Active Power Limit.
		 *
		 * <ul>
		 * <li>Interface: PV-Inverter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		GENRATOR_ACTIVE_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onInit(channel -> { //
					// on each Write to the channel -> set the value
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.setNextValue(value);
					});
				}));

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
	 * Gets the Channel for {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGeneratorMaxActivePowerChannel() {
		return this.channel(ChannelId.GENRATOR_MAX_ACTIVE_POWER);
	}

	

	/**
	 * Gets the Maximum Active Power in [WATT], range "&gt;= 0". See
	 * {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGeneratorMaxActivePower() {
		return this.getGeneratorMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorMaxActivePower(Integer value) {
		this.getGeneratorMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorMaxActivePower(int value) {
		this.getGeneratorMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGeneratorMaxReactivePowerChannel() {
		return this.channel(ChannelId.GENRATOR_MAX_REACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Reactive Power in [VAR], range "&gt;= 0". See
	 * {@link ChannelId#MAX_REACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGeneratorMaxReactivePower() {
		return this.getGeneratorMaxReactivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_REACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorMaxReactivePower(Integer value) {
		this.getGeneratorMaxReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_REACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxReactivePower(int value) {
		this.getGeneratorMaxReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGeneratorMaxApparentPowerChannel() {
		return this.channel(ChannelId.GENRATOR_MAX_APPARENT_POWER);
	}

	/**
	 * Gets the Maximum Apparent Power in [VA], range "&gt;= 0". See
	 * {@link ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGeneratorMaxApparentPower() {
		return this.getGeneratorMaxApparentPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorMaxApparentPower(Integer value) {
		this.getGeneratorMaxApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorMaxApparentPower(int value) {
		this.getGeneratorMaxApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getGeneratorActivePowerLimitChannel() {
		return this.channel(ChannelId.GENRATOR_ACTIVE_POWER_LIMIT);
	}

	/**
	 * Gets the Active Power Limit in [W]. See {@link ChannelId#ACTIVE_POWER_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGeneratorActivePowerLimit() {
		return this.getGeneratorActivePowerLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorActivePowerLimit(Integer value) {
		this.getGeneratorActivePowerLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGeneratorActivePowerLimit(int value) {
		this.getGeneratorActivePowerLimitChannel().setNextValue(value);
	}

	

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedSymmetricGenerator.class, accessMode, 100) //
				.build();
	}

	public void applyPower(int calculateChpPowerTarget);

	public void applyPower(Integer activePowerTarget);
	
}
