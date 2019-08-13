package io.openems.edge.tesla.powerwall2.core;

import java.util.Optional;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.tesla.powerwall2.battery.TeslaPowerwall2Battery;

public interface TeslaPowerwall2Core {

	public void setBattery(TeslaPowerwall2Battery battery);

	public Optional<TeslaPowerwall2Battery> getBattery();

	public enum CoreChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT));

		private final Doc doc;

		private CoreChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
