package io.openems.edge.system.fenecon.industrial.l;

import static io.openems.common.channel.AccessMode.READ_WRITE;

import java.util.stream.Stream;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public class DummyIoIndustrialL extends AbstractDummyOpenemsComponent<DummyIoIndustrialL>
		implements DigitalInput, DigitalOutput {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DIGITAL_INPUT_1(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_INPUT_2(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_INPUT_3(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_INPUT_4(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_INPUT_OUTPUT_1(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_INPUT_OUTPUT_2(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_OUTPUT_1(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)), //
		DIGITAL_OUTPUT_2(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(READ_WRITE)); //

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

	public DummyIoIndustrialL(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DummyIoIndustrialL.ChannelId.values() //
		);
		this.digitalOutputChannels = Stream.of(DummyIoIndustrialL.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == READ_WRITE) //
				.map(this::channel) //
				.toArray(BooleanWriteChannel[]::new);
	}

	@Override
	protected DummyIoIndustrialL self() {
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
