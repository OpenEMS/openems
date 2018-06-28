package io.openems.edge.battery.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Bms extends OpenemsComponent {

	// TODO what values are necessary to define a battery system? (voltage, current, temperature...)
	
	public enum GridMode {
		UNDEFINED, ON_GRID, OFF_GRID
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * State of Charge
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		/**
		 * Grid-Mode
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(new Doc().type(OpenemsType.INTEGER) //
				.option(GridMode.UNDEFINED) //
				.option(GridMode.ON_GRID) //
				.option(GridMode.OFF_GRID) //
		),
		/**
		 * Max Active Power
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		MAX_CAPACITY(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT_HOURS));

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
	 * Gets the State of Charge in [%], range 0..100 %
	 * 
	 * @return
	 */
	default Channel<Integer> getSoc() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Is the Battery On-Grid?
	 * 
	 * @return
	 */
	default Channel<Integer> getGridMode() {
		return this.channel(ChannelId.GRID_MODE);
	}

	/**
	 * Gets the maximum capacity
	 * 
	 * @return
	 */
	default Channel<Integer> getMaxCapacity() {
		return this.channel(ChannelId.MAX_CAPACITY);
	}
	
	
}
