package io.openems.edge.sma.ess.stpxx3se.battery;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sma.ess.enums.BatteryState;
import io.openems.edge.sma.ess.enums.SetControlMode;

public interface SmaBattery extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		BAT_STATUS(Doc.of(BatteryState.values())//
				.persistencePriority(PersistencePriority.HIGH)), //

		ACT_BAT_CHRG(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BAT_CHRG(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		CUR_BAT_CHA(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //
		ACT_BAT_DSCH(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		BAT_DSCH(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //
		CUR_BAT_DSCH(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)), //
		BATTERY_WARNING(Doc.of(Level.WARNING)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Warning in the battery!")), //
		BATTERY_ERROR(Doc.of(Level.FAULT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Error in the battery!")), //

		SET_CONTROL_MODE(Doc.of(SetControlMode.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //

		// SELF_CONSUMPTION_ACTIVATION(Doc.of(OpenemsType.INTEGER) //
		// .accessMode(AccessMode.READ_WRITE) //
		// .unit(Unit.NONE)), //
		;

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
	 * Gets the Channel for {@link ChannelId#CUR_BAT_CHA}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurBatChaChannel() {
		return this.channel(ChannelId.CUR_BAT_CHA);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CUR_BAT_DSCH}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getCurBatDschChannel() {
		return this.channel(ChannelId.CUR_BAT_DSCH);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_WARNING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getBatteryWarningChannel() {
		return this.channel(ChannelId.BATTERY_WARNING);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BATTERY_WARNING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBatteryWarning(boolean value) {
		this.getBatteryWarningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_ERROR}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getBatteryErrorChannel() {
		return this.channel(ChannelId.BATTERY_ERROR);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BATTERY_ERROR}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBatteryError(boolean value) {
		this.getBatteryErrorChannel().setNextValue(value);
	}

}
