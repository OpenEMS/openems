package io.openems.edge.io.test;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.edge.common.test.TestUtils.withValue;

import java.util.stream.Stream;

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
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT7(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT8(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE)), //
		INPUT_OUTPUT9(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(READ_WRITE));

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
				.filter(channelId -> channelId.doc().getAccessMode() == READ_WRITE) //
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

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT0}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput0(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT0, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT1}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput1(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT1, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT2}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput2(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT2, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT3}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput3(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT3, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT4}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput4(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT4, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT5}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput5(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT5, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT6}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput6(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT6, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT7}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput7(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT7, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT8}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput8(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT8, value);
		return this.self();
	}

	/**
	 * Set {@link Thermometer.ChannelId#INPUT_OUTPUT9}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final DummyInputOutput withInputOutput9(boolean value) {
		withValue(this, DummyInputOutput.ChannelId.INPUT_OUTPUT9, value);
		return this.self();
	}

}
