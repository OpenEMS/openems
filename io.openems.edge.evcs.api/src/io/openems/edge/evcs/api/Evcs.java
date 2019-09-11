package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

public interface Evcs extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * The Status of the EVCS charging station.
		 * 
		 * <p>
		 * Undefined, Starting, Not ready for Charging, Ready for Charging, Charging,
		 * Error, Authorization rejected.
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Status
		 * </ul>
		 */
		STATUS(Doc.of(Status.values()) //
				.accessMode(AccessMode.READ_ONLY)),

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
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Count of phases, the EV is charging with.
		 * 
		 * <p>
		 * This value is derived from the charging station or calculated during the
		 * charging.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		PHASES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

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
				.accessMode(AccessMode.READ_ONLY)),

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
				.accessMode(AccessMode.READ_ONLY)),

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
				.accessMode(AccessMode.READ_ONLY)),

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
				.accessMode(AccessMode.READ_ONLY)),

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
				.accessMode(AccessMode.READ_ONLY));

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
	 * The Status of the EVCS charging station.
	 * 
	 * @return the EnumReadChannel
	 */
	public default EnumReadChannel status() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the Charge Power in [W].
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getChargePower() {
		return this.channel(ChannelId.CHARGE_POWER);
	}

	/**
	 * Count of phases, the EV is charging with.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getPhases() {
		return this.channel(ChannelId.PHASES);
	}

	/**
	 * Maximum Power valid by the hardware.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getMaximumHardwarePower() {
		return this.channel(ChannelId.MAXIMUM_HARDWARE_POWER);
	}

	/**
	 * Minimum Power valid by the hardware.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getMinimumHardwarePower() {
		return this.channel(ChannelId.MINIMUM_HARDWARE_POWER);
	}

	/**
	 * Maximum Power defined by software.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getMaximumPower() {
		return this.channel(ChannelId.MAXIMUM_POWER);
	}

	/**
	 * Minimum Power defined by software.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getMinimumPower() {
		return this.channel(ChannelId.MINIMUM_POWER);
	}

	/**
	 * Energy that was charged during the current or last Session.
	 * 
	 * @return the IntegerReadChannel
	 */
	public default IntegerReadChannel getEnergySession() {
		return this.channel(ChannelId.ENERGY_SESSION);
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		// TODO add remaining channels
		return ModbusSlaveNatureTable.of(ManagedEvcs.class, accessMode, 100) //
				.channel(0, ChannelId.STATUS, ModbusType.UINT16) //
				.build();
	}
}
