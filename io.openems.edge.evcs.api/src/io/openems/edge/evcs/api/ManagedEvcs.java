package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ManagedEvcs extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Sets the charge power limit of the EVCS in [W].
		 * 
		 * <p>
		 * Actual charge power depends on
		 * <ul>
		 * <li>whether the electric vehicle is connected at all and ready for charging
		 * <li>hardware limit of the charging station
		 * <li>limit of electric vehicle
		 * <li>limit of power line
		 * <li>...
		 * </ul>
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Is true if the EVCS is in a EVCS-Cluster.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Boolean
		 * </ul>
		 */
		IS_CLUSTERED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Sets a Text that is shown on the display of the EVCS.
		 * 
		 * <p>
		 * Be aware that the EVCS might not have a display or the text might be
		 * restricted.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: String
		 * </ul>
		 */
		SET_DISPLAY_TEXT(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Sets a request for a charge power. The limit is not directly activated by
		 * this call.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER_REQUEST(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Sets the energy limit for the current or next session in [Wh].
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		SET_ENERGY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE));

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
	 * Returns the modbus table for this nature.
	 * 
	 * @param accessMode accessMode
	 * @return nature table
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		// TODO add remaining channels
		return ModbusSlaveNatureTable.of(ManagedEvcs.class, accessMode, 100) //
				.uint16Reserved(0) //
				.uint16Reserved(1) //
				.channel(2, ChannelId.SET_CHARGE_POWER_LIMIT, ModbusType.UINT16) //
				.channel(3, ChannelId.SET_DISPLAY_TEXT, ModbusType.STRING16) //
				.uint16Reserved(19) //
				.uint16Reserved(20) //
				.uint16Reserved(21) //
				.build();
	}

	/**
	 * Sets the charge power limit of the EVCS in [W].
	 * 
	 * @return the IntegerWriteChannel
	 */
	public default IntegerWriteChannel setChargePowerLimit() {
		return this.channel(ChannelId.SET_CHARGE_POWER_LIMIT);
	}

	/**
	 * Sets a request for a charge power. The limit is not directly activated by
	 * this call.
	 * 
	 * @return the IntegerWriteChannel
	 */
	public default IntegerWriteChannel setChargePowerRequest() {
		return this.channel(ChannelId.SET_CHARGE_POWER_REQUEST);
	}

	/**
	 * Is true if the EVCS is in a EVCS-Cluster.
	 * 
	 * @return the BooleanReadChannel
	 */
	public default BooleanReadChannel isClustered() {
		return this.channel(ChannelId.IS_CLUSTERED);
	}

	/**
	 * Sets a Text that is shown on the display of the EVCS.
	 * 
	 * @return the StringWriteChannel
	 */
	public default StringWriteChannel setDisplayText() {
		return this.channel(ChannelId.SET_DISPLAY_TEXT);
	}

	/**
	 * Sets the energy limit for the current or next session in [Wh].
	 * 
	 * @return the IntegerWriteChannel
	 */
	public default IntegerWriteChannel setEnergyLimit() {
		return this.channel(ChannelId.SET_ENERGY_LIMIT);
	}
}
