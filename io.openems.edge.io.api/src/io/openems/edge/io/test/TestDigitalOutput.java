package io.openems.edge.io.test;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;

/**
 * Provides a simple, simulated DigitalOutput component that can be used
 * together with the OpenEMS Component test framework.
 */
public class TestDigitalOutput extends AbstractOpenemsComponent implements DigitalOutput {

	private final BooleanWriteChannel digitalOutput0 = new BooleanWriteChannel(this,
			TestDigitalOutput.ChannelId.DIGITAL_OUTPUT_0);

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		DIGITAL_OUTPUT_0(new Doc());

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public TestDigitalOutput(String id) {
		this.addChannel(this.digitalOutput0);
		super.activate(null, "", id, true);
	}

	@Override
	public WriteChannel<Boolean>[] digitalOutputChannels() {
		return new BooleanWriteChannel[] { this.digitalOutput0 };
	}

}
