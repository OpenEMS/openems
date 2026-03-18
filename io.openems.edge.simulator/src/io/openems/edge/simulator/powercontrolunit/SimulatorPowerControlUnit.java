package io.openems.edge.simulator.powercontrolunit;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SimulatorPowerControlUnit extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
