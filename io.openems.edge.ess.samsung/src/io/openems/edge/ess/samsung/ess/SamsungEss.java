package io.openems.edge.ess.samsung.ess;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;

public interface SamsungEss extends SymmetricEss, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Warning when one or more Inverters are not reachable.
		 *
		 * <ul>
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Samsung ESS not reachable!")),

		/**
		 * Describes the collection time. This represents the timestamp at which data
		 * was collected or recorded.
		 * 
		 * <ul>
		 * <li>Type: String</li>
		 * </ul>
		 */
		COLEC_TM(Doc.of(OpenemsType.STRING) //
				.text("Collection Time")),

		/**
		 * Measures the power imported from or exported to the grid.
		 * 
		 * <ul>
		 * <li>Type: Double</li>
		 * </ul>
		 */
		GRID_PW(Doc.of(OpenemsType.DOUBLE) //
				.text("Grid Power")),

		/**
		 * Measures the power produced by photovoltaic (PV) panels.
		 * 
		 * <ul>
		 * <li>Type: Double</li>
		 * </ul>
		 */
		PV_PW(Doc.of(OpenemsType.DOUBLE) //
				.text("PV Power")),

		/**
		 * Indicates the absolute discharge power.
		 * 
		 * <ul>
		 * <li>Type: Double</li>
		 * </ul>
		 */
		ABS_DSC_POWER(Doc.of(OpenemsType.DOUBLE) //
				.text("Absolute Discharge Power")),

		/**
		 * Measures the total power consumption of the system or facility.
		 * 
		 * <ul>
		 * <li>Type: Double</li>
		 * </ul>
		 */
		CONS_PW(Doc.of(OpenemsType.DOUBLE) //
				.text("Consumption Power")),

		/**
		 * Reports the current state of charge of the battery. This percentage reflects
		 * the remaining energy capacity of the battery.
		 * 
		 * <ul>
		 * <li>Type: Integer</li>
		 * </ul>
		 */
		BT_SOC(Doc.of(OpenemsType.INTEGER) //
				.text("Battery State of Charge")),

		/**
		 * Accumulates the total energy consumed actively by the system or facility.
		 * This parameter is typically measured in kilowatt-hours.
		 * 
		 * <ul>
		 * <li>Type: Integer</li>
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.text("Active Consumption Energy")),

		/**
		 * Accumulates the total energy produced actively by the system. This value is
		 * crucial for calculating the efficiency and output of energy production
		 * systems.
		 * 
		 * <ul>
		 * <li>Type: Integer</li>
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.text("Active Production Energy"));

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
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_PW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridPw(double value) {
		this.channel(ChannelId.GRID_PW).setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_PW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPvPw(double value) {
		this.channel(ChannelId.PV_PW).setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_PW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAbsPcsPw(double value) {
		this.channel(ChannelId.ABS_DSC_POWER).setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONS_PW} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsPw(double value) {
		this.channel(ChannelId.CONS_PW).setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BT_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBtSoc(int value) {
		this.channel(ChannelId.BT_SOC).setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

}
