package io.openems.edge.simulator.io;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public interface SimulatorIoDigitalInputOutput extends DigitalInput, DigitalOutput, OpenemsComponent {

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
