package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.component.OpenemsComponent;

public class EnumWriteChannel extends EnumReadChannel implements WriteChannel<Integer> {

	public static class MirrorToDebugChannel implements Consumer<Channel<Integer>> {

		private final Logger log = LoggerFactory.getLogger(MirrorToDebugChannel.class);

		private final ChannelId targetChannelId;

		public MirrorToDebugChannel(ChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<Integer> channel) {
			if (!(channel instanceof EnumWriteChannel)) {
				this.log.error("Channel [" + channel.address()
						+ "] is not an EnumWriteChannel! Unable to register \"onSetNextWrite\"-Listener!");
				return;
			}

			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((EnumWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	}

	protected EnumWriteChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc,
			OptionsEnum optionsEnum) {
		super(component, channelId, channelDoc, optionsEnum);
	}

	private Optional<Integer> nextWriteValueOpt = Optional.empty();

	/**
	 * Updates the 'next' write value of Channel from an Enum-String value.
	 *
	 * @param value the name of the option as string
	 * @throws OpenemsNamedException one error
	 */
	public void setNextWriteValue(String value) throws OpenemsNamedException {
		try {
			this.setNextWriteValue(this.channelDoc().getOptionValueFromString(value));
		} catch (IllegalArgumentException e) {
			throw new OpenemsException(
					"Unable to set value for Channel [" + this.channelId() + "] from Enum [" + value + "]");
		}
	}

	/**
	 * Updates the 'next' write value of Channel from an Enum value.
	 *
	 * @param value the OptionsEnum value
	 * @throws OpenemsNamedException on error
	 */
	public void setNextWriteValue(OptionsEnum value) throws OpenemsNamedException {
		this.setNextWriteValue(value.getValue());
	}

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Integer value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Integer> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Integer, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Integer, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
