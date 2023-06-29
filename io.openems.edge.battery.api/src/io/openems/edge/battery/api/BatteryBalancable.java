package io.openems.edge.battery.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

public interface BatteryBalancable extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Set balancing target voltage.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * </ul>
		 */
		SET_BALANCING_TARGET_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Set balancing conditions fulfilled.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * </ul>
		 */
		SET_BALANCING_CONDITIONS_FULFILLED(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Set open circuit voltage reached at all the batteries.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * </ul>
		 */
		SET_OCV_REACHED_AT_ALL_THE_BATTERIES(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Set balancing running.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * </ul>
		 */
		SET_BALANCING_RUNNING(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Balancing minimum cell voltage.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * </ul>
		 */
		BALANCING_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.accessMode(AccessMode.READ_ONLY)), //
		/**
		 * Open circuit voltage reached.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#BOOLEAN}
		 * </ul>
		 */
		OCV_REACHED(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		/**
		 * Balancing still running.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#BOOLEAN}
		 * </ul>
		 */
		BALANCING_STILL_RUNNING(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		/**
		 * Balancing condition.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryBalancable}
		 * <li>Type: {@link OpenemsType#BOOLEAN}
		 * </ul>
		 */
		BALANCING_CONDITION(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)); //

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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(BatteryBalancable.class, accessMode, 10) //
				.channel(0, ChannelId.SET_BALANCING_TARGET_VOLTAGE, ModbusType.FLOAT32) //
				.channel(2, ChannelId.SET_BALANCING_CONDITIONS_FULFILLED, ModbusType.UINT16) //
				.channel(3, ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES, ModbusType.UINT16) //
				.channel(4, ChannelId.SET_BALANCING_RUNNING, ModbusType.UINT16) //
				.channel(5, ChannelId.BALANCING_MIN_CELL_VOLTAGE, ModbusType.FLOAT32) //
				.channel(7, ChannelId.OCV_REACHED, ModbusType.UINT16) //
				.channel(8, ChannelId.BALANCING_STILL_RUNNING, ModbusType.UINT16) //
				.channel(9, ChannelId.BALANCING_CONDITION, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingTargetVoltageChannel() {
		return this.channel(ChannelId.SET_BALANCING_TARGET_VOLTAGE);
	}

	/**
	 * See {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingTargetVoltage() {
		return this.getBalancingTargetVoltageChannel().value();
	}

	/**
	 * See {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingTargetVoltage(Integer value) throws OpenemsNamedException {
		this.getBalancingTargetVoltageChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingConditionsFullfilledChannel() {
		return this.channel(ChannelId.SET_BALANCING_CONDITIONS_FULFILLED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingConditionsFullfilled() {
		return this.getBalancingConditionsFullfilledChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingConditionsFullfilled(Integer value) throws OpenemsNamedException {
		this.getBalancingConditionsFullfilledChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getOcvReachedAtAllTheBatteriesChannel() {
		return this.channel(ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOcvReachedAtAllTheBatteries() {
		return this.getOcvReachedAtAllTheBatteriesChannel().value();
	}

	/**
	 * Writes the value to the
	 * {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOcvReachedAtAllTheBatteries(Integer value) throws OpenemsNamedException {
		this.getOcvReachedAtAllTheBatteriesChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_RUNNING}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingRunningChannel() {
		return this.channel(ChannelId.SET_BALANCING_RUNNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_RUNNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingRunning() {
		return this.getBalancingRunningChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#SET_BALANCING_RUNNING} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingRunning(Integer value) throws OpenemsNamedException {
		this.getBalancingRunningChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Integer> getBalancingMinCellVoltageChannel() {
		return this.channel(ChannelId.BALANCING_MIN_CELL_VOLTAGE);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingMinCellVoltage() {
		return this.getBalancingMinCellVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OCV_REACHED}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getOcvReachedChannel() {
		return this.channel(ChannelId.OCV_REACHED);
	}

	/**
	 * Gets the {@link ChannelId#OCV_REACHED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getOcvReached() {
		return this.getOcvReachedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_STILL_RUNNING}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getBalancingStillRunningChannel() {
		return this.channel(ChannelId.BALANCING_STILL_RUNNING);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_STILL_RUNNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBalancingStillRunning() {
		return this.getBalancingStillRunningChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_CONDITION}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getBalancingConditionChannel() {
		return this.channel(ChannelId.BALANCING_CONDITION);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_CONDITION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBalancingCondition() {
		return this.getBalancingConditionChannel().value();
	}
}
