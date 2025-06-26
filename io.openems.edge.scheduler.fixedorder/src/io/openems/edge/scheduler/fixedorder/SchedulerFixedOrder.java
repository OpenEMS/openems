package io.openems.edge.scheduler.fixedorder;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.scheduler.api.Scheduler;

public interface SchedulerFixedOrder extends Scheduler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
}
