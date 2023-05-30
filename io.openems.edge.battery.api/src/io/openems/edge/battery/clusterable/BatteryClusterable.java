package io.openems.edge.battery.clusterable;

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
import io.openems.edge.common.startstop.StartStop;

public interface BatteryClusterable extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Cooling valve state.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Boolean
		 * </ul>
		 */
		COOLING_VALVE_STATE(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Set balancing target voltage.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Integer
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
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Integer
		 * </ul>
		 */
		SET_BALANCING_CONDITIONS_FULFILLED(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Set open circuit voltage reached at all the batteries.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Integer
		 * </ul>
		 */
		SET_OCV_REACHED_AT_ALL_THE_BATTERIES(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Set balancing running.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Integer
		 * </ul>
		 */
		SET_BALANCING_RUNNING(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * Balancing minimum cell voltage.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Integer
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
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Boolean
		 * </ul>
		 */
		OCV_REACHED(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		/**
		 * Balancing still running.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Boolean
		 * </ul>
		 */
		BALANCING_STILL_RUNNING(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		/**
		 * Balancing condition.
		 *
		 * <ul>
		 * <li>Interface: {@link BatteryClusterable}
		 * <li>Type: Boolean
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
		return ModbusSlaveNatureTable.of(BatteryClusterable.class, accessMode, 10) //
				.channel(0, ChannelId.COOLING_VALVE_STATE, ModbusType.UINT16) //
				.channel(1, ChannelId.SET_BALANCING_TARGET_VOLTAGE, ModbusType.FLOAT32) //
				.channel(3, ChannelId.SET_BALANCING_CONDITIONS_FULFILLED, ModbusType.UINT16) //
				.channel(4, ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES, ModbusType.UINT16) //
				.channel(5, ChannelId.SET_BALANCING_RUNNING, ModbusType.UINT16) //
				.channel(6, ChannelId.BALANCING_MIN_CELL_VOLTAGE, ModbusType.FLOAT32) //
				.channel(8, ChannelId.OCV_REACHED, ModbusType.UINT16) //
				.channel(9, ChannelId.BALANCING_STILL_RUNNING, ModbusType.UINT16) //
				.channel(10, ChannelId.BALANCING_CONDITION, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getCoolingValveStateChannel() {
		return this.channel(ChannelId.COOLING_VALVE_STATE);
	}

	/**
	 * Gets the {@link ChannelId#COOLING_VALVE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCoolingValveState() {
		return this.getCoolingValveStateChannel().value();
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

	/**
	 * A helper method in determining when to begin heating the battery. The serial
	 * cluster will begin to heat up the batteries if the all battery temperatures is
	 * below 10 degrees and they are all started.
	 * 
	 * @param value true for start heating.
	 */
	public void setHeatingTarget(boolean value);

	/**
	 * Start the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void startHeating() {
		this.setHeatingTarget(true);
	}

	/**
	 * Stop the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void stopHeating() {
		this.setHeatingTarget(false);
	}

	/**
	 * A helper in deciding whether to turn on the main contactor of the batteries.
	 * Particularly for parallel clusters, it is necessary. By
	 * {@link StartStop#START} Batteries can be started in order to communicate with
	 * them, but after the voltage comparison decision has been made, the primary
	 * contactor of the second, third... batteries can be closed.
	 * 
	 * @param value true to turn on the main contactor
	 */
	public void setMainContactorTarget(boolean value);
}
