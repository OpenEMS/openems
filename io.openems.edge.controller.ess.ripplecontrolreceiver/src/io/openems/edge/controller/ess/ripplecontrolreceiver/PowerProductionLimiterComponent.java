package io.openems.edge.controller.ess.ripplecontrolreceiver;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

public interface PowerProductionLimiterComponent extends PowerProductionLimiter, OpenemsComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		RESTRICTION(Doc.of(INTEGER)
				.unit(Unit.WATT)
				.persistencePriority(HIGH)), //

		CUMULATED_RESTRICTION_TIME(Doc.of(LONG)//
				.unit(Unit.CUMULATED_SECONDS)//
				.persistencePriority(HIGH)); //

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default Integer getGridFeedInLimit() {
		return (Integer) this.channel(ChannelId.RESTRICTION).value().orElse(null);
	}
}
