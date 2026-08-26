package io.openems.edge.evcs.abl;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsAbl extends Evcs, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		ERROR(Doc.of(Level.FAULT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.translationKey(EvcsAbl.class, "errGeneral")), //
		CHARGE_POINT_STATE(Doc.of(AblStatus.values())//
				.accessMode(AccessMode.READ_ONLY)//
				.persistencePriority(PersistencePriority.HIGH)), //
		CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.accessMode(AccessMode.READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * metered active Production Energy.
		 */
		RAW_ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)//
				.persistencePriority(PersistencePriority.HIGH));

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
	 * Gets the Channel for {@link ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	default Channel<AblStatus> getChargePointStateChannel() {
		return this.channel(ChannelId.CHARGE_POINT_STATE);
	}

	/**
	 * Gets the {@link ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the {@link AblStatus}
	 */
	default AblStatus getChargePointState() {
		return this.getChargePointStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getCurrentLimitChannel() {
		return this.channel(ChannelId.CURRENT_LIMIT);
	}

	/**
	 * Sets a value into the CurrentLimit register. See
	 * {@link ChannelId#CURRENT_LIMIT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setCurrentLimit(int value) throws OpenemsError.OpenemsNamedException {
		this.getCurrentLimitChannel().setNextWriteValue(value);
	}

}
