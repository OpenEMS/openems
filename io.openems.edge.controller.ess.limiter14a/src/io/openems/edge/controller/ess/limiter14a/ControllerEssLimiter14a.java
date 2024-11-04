package io.openems.edge.controller.ess.limiter14a;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssLimiter14a extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		RESTRICTION_MODE(Doc.of(RestrictionMode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		
		CUMULATED_RESTRICTION_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)); //
		
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
	 * Gets the Channel for {@link ChannelId#RESTRICTION_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getRestrictionModeChannel() {
		return this.channel(ChannelId.RESTRICTION_MODE);
	}

	/**
	 * Gets the Status. See {@link ChannelId#RESTRICTION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Boolean getRestrictionMode() {
		return this.getRestrictionModeChannel().value().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RESTRICTION_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRestrictionMode(boolean value) {
		this.getRestrictionModeChannel().setNextValue(value);
	}

}
