package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Evcs extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

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
		 * Minimum Power valid by the hardware.
		 * In the cases that the EVCS can't be controlled, the Minimum will be the maximum too.
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MINIMUM_HARDWARE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Maximum Power valid by the hardware.
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAXIMUM_HARDWARE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Maximum Power that should be charged calculated by the controller
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAXIMUM_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),
		
		/**
		 * Variable minimum power that should be charged, configured in a controller
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MINIMUM_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),
		
		/**
		 * Energy that was charged during this Session 
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Writable
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
	 * Gets the Charge Power in [W].
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getChargePower() {
		return this.channel(ChannelId.CHARGE_POWER);
	}

	/**
	 * Gets the maximum Charge Power in [W] valid by the Hardware.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getMaximumHardwarePower() {
		return this.channel(ChannelId.MAXIMUM_HARDWARE_POWER);
	}

	/**
	 * Gets the minimum Charge Power in [W] valid by the Hardware.
	 * In the cases that the EVCS can't be controlled, the Minimum will be the maximum too.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getMinimumHardwarePower() {
		return this.channel(ChannelId.MINIMUM_HARDWARE_POWER);
	}
	
	/**
	 * Gets the maximum Charge Power that will be used by the charging station.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getMaximumPower() {
		return this.channel(ChannelId.MAXIMUM_POWER);
	}
	
	/**
	 * Gets the minimum Charge Power that will mostly used by the charging station.
	 * Exceptions when a minimum is not guaranteed could be the excess of the grid.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getMinimumPower() {
		return this.channel(ChannelId.MINIMUM_POWER);
	}
	
	/**
	 * Gets the current energy in [Wh], charged in this session.
	 * 
	 * @return the Channel
	 */
	public default Channel<Integer> getEnergySession() {
		return this.channel(ChannelId.ENERGY_SESSION);
	}
}
