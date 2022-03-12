package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanWriteChannel extends BooleanReadChannel implements WriteChannel<Boolean> {

	public static class MirrorToDebugChannel implements Consumer<Channel<Boolean>> {

		private final Logger log = LoggerFactory.getLogger(MirrorToDebugChannel.class);

		private final ChannelId targetChannelId;

		public MirrorToDebugChannel(ChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<Boolean> channel) {
			if (!(channel instanceof BooleanWriteChannel)) {
				this.log.error("Channel [" + channel.address()
						+ "] is not an BooleanWriteChannel! Unable to register \"onSetNextWrite\"-Listener!");
				return;
			}

			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((BooleanWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	}

	private Optional<Boolean> nextWriteValueOpt = Optional.empty();

	protected BooleanWriteChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Boolean value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Boolean> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 *
	 * @return
	 */
	@Override
	public List<ThrowingConsumer<Boolean, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Boolean, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
