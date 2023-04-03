package io.openems.edge.common.startstoppratelimited;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface RateLimitedStartStoppable  extends StartStoppable {
	
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Start/Stop.
		 *
		 * <ul>
		 * <li>Interface: StartStoppable
		 * <li>Type: {@link StartStop}
		 * <li>Range: 0=Undefined, 1=Start, 2=Stop
		 * </ul>
		 */
		REMAINING_START_COUNT(Doc.of(OpenemsType.INTEGER));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	int getRemainingStarts();
	
	StartFrequency getMaxStartFrequency();
}
