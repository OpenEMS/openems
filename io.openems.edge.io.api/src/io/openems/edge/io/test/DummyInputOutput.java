package io.openems.edge.io.test;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

/**
 * Provides a simple, simulated Digital Input/Output component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyInputOutput extends AbstractOpenemsComponent implements DigitalInput, DigitalOutput {

	private final BooleanWriteChannel[] ioChannels;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		INPUT_OUTPUT_0(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT_2(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT_3(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), ///
		INPUT_OUTPUT_4(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), ///
		INPUT_OUTPUT_5(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), ///
		INPUT_OUTPUT_6(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), ///
		INPUT_OUTPUT_7(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT_8(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT_9(Doc.of(OpenemsType.BOOLEAN).//
				accessMode(AccessMode.READ_WRITE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DummyInputOutput(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		this.ioChannels = new BooleanWriteChannel[] { //
				this.channel(ChannelId.INPUT_OUTPUT_0), //
				this.channel(ChannelId.INPUT_OUTPUT_1), //
				this.channel(ChannelId.INPUT_OUTPUT_2), //
				this.channel(ChannelId.INPUT_OUTPUT_3), //
				this.channel(ChannelId.INPUT_OUTPUT_4), //
				this.channel(ChannelId.INPUT_OUTPUT_5), //
				this.channel(ChannelId.INPUT_OUTPUT_6), //
				this.channel(ChannelId.INPUT_OUTPUT_7), //
				this.channel(ChannelId.INPUT_OUTPUT_8), //
				this.channel(ChannelId.INPUT_OUTPUT_9) //
		};
		super.activate(new DummyComponentContext(), id, "", true);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.ioChannels;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.ioChannels;
	}

}
