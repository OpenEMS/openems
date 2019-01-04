package io.openems.edge.io.test;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

/**
 * Provides a simple, simulated Digital Input/Output component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyInputOutput extends AbstractOpenemsComponent implements DigitalInput, DigitalOutput {

	private final BooleanWriteChannel[] channels;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		INPUT_OUTPUT_0(new Doc()), //
		INPUT_OUTPUT_1(new Doc()), //
		INPUT_OUTPUT_2(new Doc()), //
		INPUT_OUTPUT_3(new Doc()), //
		INPUT_OUTPUT_4(new Doc()), //
		INPUT_OUTPUT_5(new Doc()), //
		INPUT_OUTPUT_6(new Doc()), //
		INPUT_OUTPUT_7(new Doc()), //
		INPUT_OUTPUT_8(new Doc()), //
		INPUT_OUTPUT_9(new Doc());

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public DummyInputOutput(String id) {
		this.channels = new BooleanWriteChannel[] {
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_0), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_1), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_2), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_3), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_4), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_5), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_6), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_7), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_8), //
				new BooleanWriteChannel(this, DummyInputOutput.ChannelId.INPUT_OUTPUT_9) //
		};
		for (BooleanWriteChannel channel : this.channels) {
			channel.nextProcessImage();
			this.addChannel(channel);
		}
		super.activate(null, "", id, true);
	}

	@Override
	public WriteChannel<Boolean>[] digitalOutputChannels() {
		return this.channels;
	}

	@Override
	public Channel<Boolean>[] digitalInputChannels() {
		return this.channels;
	}

}
