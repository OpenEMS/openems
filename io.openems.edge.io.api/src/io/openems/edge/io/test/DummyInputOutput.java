package io.openems.edge.io.test;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

/**
 * Provides a simple, simulated Digital Input/Output component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyInputOutput extends AbstractDummyOpenemsComponent<DummyInputOutput>
		implements DigitalInput, DigitalOutput {

	private final BooleanWriteChannel[] ioChannels;

	public DummyInputOutput(String id) {
		this(id, "INPUT_OUTPUT", 0, 10);
	}

	public DummyInputOutput(String id, String prefix, int start, int numberOfIOs) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				DigitalOutput.ChannelId.values() //
		);

		this.ioChannels = new BooleanWriteChannel[numberOfIOs];
		for (int i = 0; i < numberOfIOs; i++) {
			this.ioChannels[i] = (BooleanWriteChannel) this
					.addChannel(new ChannelIdImpl(prefix + "_" + (i + start), Doc.of(OpenemsType.BOOLEAN).//
							accessMode(AccessMode.READ_WRITE)));
		}
	}

	@Override
	protected DummyInputOutput self() {
		return this;
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
