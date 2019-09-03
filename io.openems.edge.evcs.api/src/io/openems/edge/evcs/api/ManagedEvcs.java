package io.openems.edge.evcs.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

@ProviderType
public interface ManagedEvcs extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Status .
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Status
		 * </ul> 
		 */
		STATUS(Doc.of(Status.values())
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Current charging target .
		 * What is set and schould be charged by the Charging Station
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 */
		CURR_CHARGING_TARGET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),
		
		/**
		 * Count of ladders, the car is charging with .
		 * Getting from the charging station
		 * or calculated during the charging
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * 
		 */
		PHASES(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)), //

		/**
		 * Is true if the evcs is clustered
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Boolean
		 */
		IS_CLUSTERED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)), //
		
		/**
		 * Set Charge Power.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),
		
		/**
		 * Requests someone to set the Charge Power if that amount is possible.
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
		 * Set Display Text.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: String
		 * </ul>
		 */
		SET_DISPLAY_TEXT(Doc.of(OpenemsType.STRING) //
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
	
	
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedEvcs.class, accessMode, 100) //
				.uint16Reserved(0) //
				.uint16Reserved(1)
				.channel(2, ChannelId.SET_CHARGE_POWER, ModbusType.UINT16)
				.channel(3, ChannelId.SET_DISPLAY_TEXT, ModbusType.STRING16)
				.uint16Reserved(19)
				.uint16Reserved(20)
				.uint16Reserved(21)
				.channel(22, ChannelId.STATUS, ModbusType.UINT16)
				.build();
	}

	/**
	 * Sets the allowed maximum charge power of the EVCS in [W].
	 * 
	 * <p>
	 * Actual charge power depends on
	 * <ul>
	 * <li>whether the electric vehicle is connected at all and ready for charging
	 * <li>limitation of electric vehicle
	 * <li>limitation of power line
	 * <li>...
	 * </ul>
	 * 
	 * @return the WriteChannel
	 */
	public default WriteChannel<Integer> setChargePower() {
		return this.channel(ChannelId.SET_CHARGE_POWER);
	}
	
	/**
	 * Requests someone to set the charge power of the EVCS in [W] if this amount is possible.
	 * 
	 * <p>
	 * Actual charge power depends on
	 * <ul>
	 * <li>whether the electric vehicle is connected at all and ready for charging
	 * <li>limitation of electric vehicle
	 * <li>limitation of power line
	 * <li>...
	 * </ul>
	 * 
	 * @return the WriteChannel
	 */
	public default WriteChannel<Integer> setChargePowerRequest() {
		return this.channel(ChannelId.SET_CHARGE_POWER_REQUEST);
	}

	/**
	 * Sets a Text that is shown at the display of the EVCS. Be aware that the EVCS
	 * might not have a display or the text might be restricted.
	 * 
	 * @return the WriteChannel
	 */
	public default WriteChannel<String> setDisplayText() {
		return this.channel(ChannelId.SET_DISPLAY_TEXT);
	}
	
	public default Channel<Status> status(){
		return this.channel(ChannelId.STATUS);
	}	
	
	public default Channel<Integer> getCurrChargingTarget() {
		return this.channel(ChannelId.CURR_CHARGING_TARGET);
	}	
	
	public default Channel<Integer> getPhases() {
		return this.channel(ChannelId.PHASES);
	}	
	
	public default Channel<Integer> isClustered() {
		return this.channel(ChannelId.IS_CLUSTERED);
	}	
}
