package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.WriteChannel;
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
				.onInit(new PowerConstraint("SetActivePowerL1Equals", Phase.L1, Pwr.ACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetActivePowerL2Equals", Phase.L2, Pwr.ACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetActivePowerL3Equals", Phase.L3, Pwr.ACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetReactivePowerL1Equals", Phase.L1, Pwr.REACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetReactivePowerL2Equals", Phase.L2, Pwr.REACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetReactivePowerL2Equals", Phase.L3, Pwr.REACTIVE, Relationship.EQUALS))), //
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
				.onInit(new PowerConstraint("SetActivePowerL1LessOrEquals", Phase.L1, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetActivePowerL2LessOrEquals", Phase.L2, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetActivePowerL3LessOrEquals", Phase.L3, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetActivePowerL1GreaterOrEquals", Phase.L1, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetActivePowerL2GreaterOrEquals", Phase.L2, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetActivePowerL3GreaterOrEquals", Phase.L3, Pwr.ACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL1LessOrEquals", Phase.L1, Pwr.REACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL2LessOrEquals", Phase.L2, Pwr.REACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL3LessOrEquals", Phase.L3, Pwr.REACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL1GreaterOrEquals", Phase.L1, Pwr.REACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL2GreaterOrEquals", Phase.L2, Pwr.REACTIVE,
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
				.onInit(new PowerConstraint("SetReactivePowerL3GreaterOrEquals", Phase.L3, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS))), //
		/**
		 * Holds settings of Active Power L1 for debugging
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
				.unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
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
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Holds settings of Active Power L2 for debugging
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
				.unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
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
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Holds settings of Active Power L1 for debugging
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
				.unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
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
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

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
		int activePowerBy3 = activePower / 3;
		int reactivePowerBy3 = reactivePower / 3;
		this.applyPower(activePowerBy3, reactivePowerBy3, activePowerBy3, reactivePowerBy3, activePowerBy3,
				reactivePowerBy3);
	}

	/**
	 * Apply the calculated Power
	 * 
	 * @param activePowerL1
	 * @param activePowerL2
	 * @param activePowerL3
	 * @param reactivePowerL1
	 * @param reactivePowerL2
	 * @param reactivePowerL3
	 */
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException;

	/**
	 * Gets the Set Active Power L1 Equals in [W]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL1Equals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_EQUALS);
	}

	/**
	 * Gets the Set Active Power L2 Equals in [W]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL2Equals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_EQUALS);
	}

	/**
	 * Gets the Set Active Power L3 Equals in [W]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL3Equals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L1 Equals in [var]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL1Equals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L2 Equals in [var]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL2Equals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L3 Equals in [var]
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL3Equals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_EQUALS);
	}

	/**
	 * Gets the Set Active Power L1 Less Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL1LessOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power L2 Less Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL2LessOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power L3 Less Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL3LessOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power L1 Greater Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL1GreaterOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L1_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power L2 Greater Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL2GreaterOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L2_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Active Power L3 Greater Or Equals in [W].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetActivePowerL3GreaterOrEquals() {
		return this.channel(ChannelId.SET_ACTIVE_POWER_L3_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L1 Less Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL1LessOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L2 Less Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL2LessOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L3 Less Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL3LessOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_LESS_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L1 Greater Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL1GreaterOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L1_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L2 Greater Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL2GreaterOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L2_GREATER_OR_EQUALS);
	}

	/**
	 * Gets the Set Reactive Power L3 Greater Or Equals in [var].
	 * 
	 * @return
	 */
	default WriteChannel<Integer> getSetReactivePowerL3GreaterOrEquals() {
		return this.channel(ChannelId.SET_REACTIVE_POWER_L3_GREATER_OR_EQUALS);
	}
}
