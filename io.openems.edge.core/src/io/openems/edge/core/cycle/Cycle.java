package io.openems.edge.core.cycle;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface Cycle {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Actual, measured Cycle-Time in [ms].
		 * 
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: Integer
		 * </ul>
		 */
		MEASURED_CYCLE_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

}
