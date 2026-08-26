package io.openems.edge.evcs.heidelberg.energy;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsHeidelbergEnergy extends Evcs, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		ERROR(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.translationKey(EvcsHeidelbergEnergy.class, "errGeneral")), //

		/**
		 * See Modbus specification for details on the Mode 3 state.
		 */
		HEIDELBERG_STATE(Doc.of(OpenemsType.INTEGER)), //
		WATCHDOG_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		STANDBY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
		);

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
	 * Gets the Channel for {@link ChannelId#ERROR}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getErrorChannel() {
		return this.channel(ChannelId.ERROR);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ERROR}.
	 *
	 * @param value the next value
	 */
	public default void _setError(boolean value) {
		this.getErrorChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WATCHDOG_TIME}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getWatchdogTimeChannel() {
		return this.channel(ChannelId.WATCHDOG_TIME);
	}

	/**
	 * Internal method to set the 'nextWriteValue' on {@link ChannelId#STANDBY}.
	 *
	 * @param value the next value
	 */
	public default void setWatchdog(int value) throws OpenemsError.OpenemsNamedException {
		this.getWatchdogTimeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#STANDBY}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getStandbyChannel() {
		return this.channel(ChannelId.STANDBY);
	}

	/**
	 * Internal method to set the 'nextWriteValue' on {@link ChannelId#STANDBY}.
	 *
	 * @param value the next value
	 */
	public default void setStandby(int value) throws OpenemsError.OpenemsNamedException {
		this.getStandbyChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getMaxCurrentChannel() {
		return this.channel(ChannelId.MAX_CURRENT);
	}

	/**
	 * Internal method to set the 'nextWriteValue' on {@link ChannelId#MAX_CURRENT}.
	 *
	 * @param value the next value
	 */
	public default void setMaxCurrent(int value) throws OpenemsError.OpenemsNamedException {
		this.getMaxCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FAILSAFE_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getFailsafeCurrentChannel() {
		return this.channel(ChannelId.FAILSAFE_CURRENT);
	}

	/**
	 * Internal method to set the 'nextWriteValue' on
	 * {@link ChannelId#FAILSAFE_CURRENT}.
	 *
	 * @param value the next value
	 */
	public default void setFailsafeCurrent(int value) throws OpenemsError.OpenemsNamedException {
		this.getFailsafeCurrentChannel().setNextWriteValue(value);
	}
}