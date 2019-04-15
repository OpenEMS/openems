package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.sum.GridMode;

@ProviderType
public interface SymmetricEss extends OpenemsComponent {

	public static final String POWER_DOC_TEXT = "Negative values for Charge; positive for Discharge";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * State of Charge.
		 * 
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),
		/**
		 * Grid-Mode.
		 * 
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values())),
		/**
		 * Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Reactive Power.
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Holds the currently maximum possible apparent power. This value is commonly
		 * defined by the inverter limitations.
		 * 
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		/**
		 * Active Charge Energy.
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Active Discharge Energy.
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.SOC, ModbusType.UINT16) //
				.channel(1, ChannelId.GRID_MODE, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getSoc() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Is the Ess On-Grid?.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getGridMode() {
		return this.channel(ChannelId.GRID_MODE);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Apparent Power in [VA], range "&gt;= 0".
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMaxApparentPower() {
		return this.channel(ChannelId.MAX_APPARENT_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. Negative values for Charge; positive for
	 * Discharge.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Active Charge Energy in [Wh].
	 * 
	 * @return the Channel
	 */
	default Channel<Long> getActiveChargeEnergy() {
		return this.channel(ChannelId.ACTIVE_CHARGE_ENERGY);
	}

	/**
	 * Gets the Active Discharge Energy in [Wh].
	 * 
	 * @return the Channel
	 */
	default Channel<Long> getActiveDischargeEnergy() {
		return this.channel(ChannelId.ACTIVE_DISCHARGE_ENERGY);
	}
}
