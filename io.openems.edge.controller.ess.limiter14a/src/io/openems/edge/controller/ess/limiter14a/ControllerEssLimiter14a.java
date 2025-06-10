package io.openems.edge.controller.ess.limiter14a;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssLimiter14a extends Controller, OpenemsComponent {

	/**
	 * If RESTRICTION_MODE is true, ESS charge power is limited to 4.2 kW.
	 */
	public static final int ESS_LIMIT_14A_ENWG = -4200;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		RESTRICTION_MODE(Doc.of(BOOLEAN) //
				.persistencePriority(HIGH)), //

		CUMULATED_RESTRICTION_TIME(Doc.of(LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(HIGH)); //

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
}
