package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

public interface ManagedVehicleBattery extends ManagedEvcs, SocEvcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Determines if the ManagedVehicleBattery is used as a battery or an Evcs.
		 *
		 * <ul>
		 * <li>Interface: ManagedVehicleBattery
		 * <li>Readable
		 * <li>Type: BOOLEAN
		 * <li>Unit: on/offO
		 * </ul>
		 */
		BATTERY_MODE(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).persistencePriority(PersistencePriority.HIGH)),

		// TODO Should be the other way round: Negative for charge, positive for
		// discharge, in accordance with Ess?
		/**
		 * Active Power. Negative for discharge, positive for charge.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW)),

		/**
		 * If Battery mode is on, the requested active power is set via this channel.
		 * <ul>
		 * <li>Interface: ManagedVehicleBattery
		 * <li>Writable
		 * <li>Type: INTEGER - negative value indicate charging of the battery, positive
		 * value indicate discharge of the battery,
		 * <li>Unit: W
		 * </ul>
		 * *
		 */

		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE));

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
	 * Gets the Channel for {@link ChannelId#BATTERY_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBatteryModeChannel() {
		return this.channel(ChannelId.BATTERY_MODE);
	}

	/**
	 * Gets the Battery Mode. See {@link ChannelId#BATTERY_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBatteryMode() {
		return this.getBatteryModeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BATTERY_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBatteryMode(Boolean value) {
		this.getBatteryModeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BATTERY_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBatteryMode(boolean value) {
		this.getBatteryModeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerChannel() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Consumption (power that is
	 * 'leaving the system', e.g. feed-to-grid); positive for Production (power that
	 * is 'entering the system'). See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePower() {
		return this.getActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePower(Integer value) {
		this.getActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePower(int value) {
		this.getActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetActivePowerChannel() {
		return this.channel(ChannelId.SET_ACTIVE_POWER);
	}

	/**
	 * Gets the Battery Mode. See {@link ChannelId#SET_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetActivePower() {
		return this.getSetActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetActivePower(Integer value) {
		this.getBatteryModeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetActivePower(int value) {
		this.getBatteryModeChannel().setNextValue(value);
	}

	/**
	 * Sets the active power request of the vehicle battery in [W]. See
	 * {@link ChannelId#SET_ACTIVE_POWER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */

	public default void setActivePowerWriteValue(Integer value) throws OpenemsNamedException {
		this.getSetActivePowerChannel().setNextWriteValue(value);
	}

}
