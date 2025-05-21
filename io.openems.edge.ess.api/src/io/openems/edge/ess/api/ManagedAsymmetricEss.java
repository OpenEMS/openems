package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedAsymmetricEss extends ManagedSymmetricEss, AsymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Sets a fixed Active Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L1_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetActivePowerL1Equals", Phase.L1, Pwr.ACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed Active Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L2_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetActivePowerL2Equals", Phase.L2, Pwr.ACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed Active Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L3_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetActivePowerL3Equals", Phase.L3, Pwr.ACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed Reactive Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L1_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetReactivePowerL1Equals", Phase.L1, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed Reactive Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L2_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetReactivePowerL2Equals", Phase.L2, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed Reactive Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L3_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(
						new PowerConstraint("SetReactivePowerL2Equals", Phase.L3, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Sets a fixed maximum Active Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L1_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL1LessOrEquals", Phase.L1, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Active Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L2_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL2LessOrEquals", Phase.L2, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Active Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L3_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL3LessOrEquals", Phase.L3, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Active Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL1GreaterOrEquals", Phase.L1, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Active Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL2GreaterOrEquals", Phase.L2, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Active Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetActivePowerL3GreaterOrEquals", Phase.L3, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Reactive Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L1_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL1LessOrEquals", Phase.L1, Pwr.REACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Reactive Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L2_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL2LessOrEquals", Phase.L2, Pwr.REACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed maximum Reactive Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L3_LESS_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL3LessOrEquals", Phase.L3, Pwr.REACTIVE,
						Relationship.LESS_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Reactive Power on L1.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL1GreaterOrEquals", Phase.L1, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Reactive Power on L2.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL2GreaterOrEquals", Phase.L2, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Sets a fixed minimum Reactive Power on L3.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWrite(new PowerConstraint("SetReactivePowerL3GreaterOrEquals", Phase.L3, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Holds settings of Active Power L1 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Holds settings of Reactive Power L1 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Holds settings of Active Power L2 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Holds settings of Reactive Power L2 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Holds settings of Active Power L3 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Holds settings of Reactive Power L3 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by
		 * {@link io.openems.edge.ess.power.api.Power} just before it calls the
		 * onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedAsymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.SET_ACTIVE_POWER_L1_EQUALS, ModbusType.FLOAT32) //
				.channel(2, ChannelId.SET_ACTIVE_POWER_L2_EQUALS, ModbusType.FLOAT32) //
				.channel(4, ChannelId.SET_ACTIVE_POWER_L3_EQUALS, ModbusType.FLOAT32) //
				.channel(6, ChannelId.SET_REACTIVE_POWER_L1_EQUALS, ModbusType.FLOAT32) //
				.channel(8, ChannelId.SET_REACTIVE_POWER_L2_EQUALS, ModbusType.FLOAT32) //
				.channel(10, ChannelId.SET_REACTIVE_POWER_L3_EQUALS, ModbusType.FLOAT32) //
				.channel(12, ChannelId.SET_ACTIVE_POWER_L1_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(14, ChannelId.SET_ACTIVE_POWER_L2_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(16, ChannelId.SET_ACTIVE_POWER_L3_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(18, ChannelId.SET_REACTIVE_POWER_L1_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(20, ChannelId.SET_REACTIVE_POWER_L2_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(22, ChannelId.SET_REACTIVE_POWER_L3_LESS_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(24, ChannelId.SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(26, ChannelId.SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(28, ChannelId.SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(30, ChannelId.SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(32, ChannelId.SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.channel(34, ChannelId.SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS, ModbusType.FLOAT32) //
				.build();
	}

	@Override
	default void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		var activePowerBy3 = activePower / 3;
		var reactivePowerBy3 = reactivePower / 3;
		this.applyPower(activePowerBy3, reactivePowerBy3, activePowerBy3, reactivePowerBy3, activePowerBy3,
				reactivePowerBy3);
	}

	/**
	 * Apply the calculated Power.
	 *
	 * @param activePowerL1   the active power set-point for L1
	 * @param reactivePowerL1 the reactive power set-point for L1
	 * @param activePowerL2   the active power set-point for L2
	 * @param reactivePowerL2 the reactive power set-point for L2
	 * @param activePowerL3   the active power set-point for L3
	 * @param reactivePowerL3 the reactive power set-point for L3
	 */
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException;

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L1_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL1EqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_EQUALS);
	}

	/**
	 * Sets an Active Power Equals setpoint on L1 in [W]. Negative values for
	 * Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L1_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL1Equals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL1EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L2_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL2EqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_EQUALS);
	}

	/**
	 * Sets an Active Power Equals setpoint on L2 in [W]. Negative values for
	 * Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L2_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL2Equals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL2EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L3_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL3EqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_EQUALS);
	}

	/**
	 * Sets a Reactive Power Equals setpoint on L3 in [W]. Negative values for
	 * Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L3_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL3Equals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL3EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L1_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL1EqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_EQUALS);
	}

	/**
	 * Sets a Reactive Power Equals setpoint on L1 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L1_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL1Equals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL1EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L2_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL2EqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_EQUALS);
	}

	/**
	 * Sets a Reactive Power Equals setpoint on L2 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L2_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL2Equals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL2EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L3_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL3EqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_EQUALS);
	}

	/**
	 * Sets a Reactive Power Equals setpoint on L3 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L3_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL3Equals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL3EqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L1_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL1LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_LESS_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Less Or Equals setpoint on L1 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L1_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL1LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL1LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L2_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL2LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_LESS_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Less Or Equals setpoint on L2 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L2_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL2LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL2LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L3_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL3LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_LESS_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Less Or Equals setpoint on L3 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L3_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL3LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL3LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL1GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Greater Or Equals setpoint on L1 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL1GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL1GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL2GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Greater Or Equals setpoint on L2 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL2GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL2GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerL3GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS);
	}

	/**
	 * Sets an Active Power Greater Or Equals setpoint on L3 in [W]. Negative values
	 * for Charge; positive for Discharge. See
	 * {@link ChannelId#SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetActivePowerL3GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerL3GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L1_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL1LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_LESS_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Less Or Equals setpoint on L1 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L1_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL1LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL1LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L2_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL2LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_LESS_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Less Or Equals setpoint on L2 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L2_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL2LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL2LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_REACTIVE_POWER_L3_LESS_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL3LessOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_LESS_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Less Or Equals setpoint on L3 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L3_LESS_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL3LessOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL3LessOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL1GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Greater Or Equals setpoint on L1 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL1GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL1GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL2GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Greater Or Equals setpoint on L2 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL2GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL2GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetReactivePowerL3GreaterOrEqualsChannel() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS);
	}

	/**
	 * Sets a Reactive Power Greater Or Equals setpoint on L3 in [var]. See
	 * {@link ChannelId#SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetReactivePowerL3GreaterOrEquals(Integer value) throws OpenemsNamedException {
		this.getSetReactivePowerL3GreaterOrEqualsChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerL1Channel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the last Active Power setpoint on L1 in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePowerL1() {
		return this.getDebugSetActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL1(Integer value) {
		this.getDebugSetActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL1(int value) {
		this.getDebugSetActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerL2Channel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the last Active Power setpoint on L2 in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePowerL2() {
		return this.getDebugSetActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL2(Integer value) {
		this.getDebugSetActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL2(int value) {
		this.getDebugSetActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetActivePowerL3Channel() {
		return this.channel(ChannelId.DEBUG_SET_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the last Active Power setpoint on L3 in [W]. See
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetActivePowerL3() {
		return this.getDebugSetActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL3(Integer value) {
		this.getDebugSetActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetActivePowerL3(int value) {
		this.getDebugSetActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetReactivePowerL1Channel() {
		return this.channel(ChannelId.DEBUG_SET_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the last Reactive Power setpoint on L1 in [var]. See
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetReactivePowerL1() {
		return this.getDebugSetReactivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL1(Integer value) {
		this.getDebugSetReactivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL1(int value) {
		this.getDebugSetReactivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetReactivePowerL2Channel() {
		return this.channel(ChannelId.DEBUG_SET_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the last Reactive Power setpoint on L2 in [var]. See
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetReactivePowerL2() {
		return this.getDebugSetReactivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL2(Integer value) {
		this.getDebugSetReactivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL2(int value) {
		this.getDebugSetReactivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugSetReactivePowerL3Channel() {
		return this.channel(ChannelId.DEBUG_SET_REACTIVE_POWER_L3);
	}

	/**
	 * Gets the last Reactive Power setpoint on L3 in [var]. See
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDebugSetReactivePowerL3() {
		return this.getDebugSetReactivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL3(Integer value) {
		this.getDebugSetReactivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_SET_REACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugSetReactivePowerL3(int value) {
		this.getDebugSetReactivePowerL3Channel().setNextValue(value);
	}
}
