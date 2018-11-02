package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedAsymmetricEss extends ManagedSymmetricEss, AsymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
		SET_ACTIVE_POWER_L1_EQUALS(new Doc() //
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
		SET_ACTIVE_POWER_L2_EQUALS(new Doc() //
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
		SET_ACTIVE_POWER_L3_EQUALS(new Doc() //
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
		SET_REACTIVE_POWER_L1_EQUALS(new Doc() //
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
		SET_REACTIVE_POWER_L2_EQUALS(new Doc() //
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
		SET_REACTIVE_POWER_L3_EQUALS(new Doc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new PowerConstraint("SetReactivePowerL2Equals", Phase.L3, Pwr.REACTIVE, Relationship.EQUALS))), //
		/**
		 * Holds settings of Active Power L1 for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Holds settings of Active Power L2 for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Holds settings of Active Power L1 for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Holds settings of Reactive Power for debugging
		 * 
		 * <ul>
		 * <li>Interface: Managed Asymmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>Implementation Note: value is automatically written by {@link Power} just
		 * before it calls the onWriteListener (which writes the value to the Ess)
		 * </ul>
		 */
		DEBUG_SET_REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable() {
		return ModbusSlaveNatureTable.of(ManagedAsymmetricEss.class, 100) //
				.channel(0, ChannelId.SET_ACTIVE_POWER_L1_EQUALS, ModbusType.FLOAT32) //
				.channel(2, ChannelId.SET_ACTIVE_POWER_L2_EQUALS, ModbusType.FLOAT32) //
				.channel(4, ChannelId.SET_ACTIVE_POWER_L3_EQUALS, ModbusType.FLOAT32) //
				.channel(6, ChannelId.SET_REACTIVE_POWER_L1_EQUALS, ModbusType.FLOAT32) //
				.channel(8, ChannelId.SET_REACTIVE_POWER_L2_EQUALS, ModbusType.FLOAT32) //
				.channel(10, ChannelId.SET_REACTIVE_POWER_L3_EQUALS, ModbusType.FLOAT32) //
				.build();
	}

	@Override
	default void applyPower(int activePower, int reactivePower) {
		int activePowerBy3 = activePower / 3;
		int reactivePowerBy3 = reactivePower / 3;
		this.applyPower(activePowerBy3, reactivePowerBy3, activePowerBy3, reactivePowerBy3, activePowerBy3,
				reactivePowerBy3);
	}

	/**
	 * Apply the calculated Power
	 * 
	 * @param activePower
	 * @param reactivePower
	 */
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3);

}
