package io.openems.edge.io.test;

import java.util.stream.Stream;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		INPUT_OUTPUT0(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		INPUT_OUTPUT9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private final BooleanWriteChannel[] digitalOutputChannels;

	public DummyInputOutput(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				ChannelId.values() //
		);
		this.digitalOutputChannels = Stream.of(ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_WRITE) //
				.map(this::channel) //
				.toArray(BooleanWriteChannel[]::new);
	}

	@Override
	protected DummyInputOutput self() {
		return this;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.digitalOutputChannels;
	}

}
