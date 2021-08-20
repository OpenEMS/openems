package io.openems.edge.common.channel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class LongWriteChannel extends LongReadChannel implements WriteChannel<Long> {

	public static class MirrorToDebugChannel implements Consumer<Channel<Long>> {

		private final Logger log = LoggerFactory.getLogger(MirrorToDebugChannel.class);

		private final ChannelId targetChannelId;

		public MirrorToDebugChannel(ChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<Long> channel) {
			if (!(channel instanceof LongWriteChannel)) {
				this.log.error("Channel [" + channel.address()
						+ "] is not an LongWriteChannel! Unable to register \"onSetNextWrite\"-Listener!");
				return;
			}

			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((LongWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	}

	protected LongWriteChannel(OpenemsComponent component, ChannelId channelId, LongDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Long> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Long value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Long> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Long, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Long, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}

	/**
	 * An object that holds information about the write target of this Channel, i.e.
	 * a Modbus Register or REST-Api endpoint address. Defaults to null.
	 */
	private Object writeTarget = null;

	@Override
	public <WRITE_TARGET> void setWriteTarget(WRITE_TARGET writeTarget) throws IllegalArgumentException {
		if (this.writeTarget != null && writeTarget != null && !Objects.equals(this.writeTarget, writeTarget)) {
			throw new IllegalArgumentException("Unable to set write target [" + writeTarget.toString()
					+ "]. Channel already has a write target [" + this.writeTarget.toString() + "]");
		}
		this.writeTarget = writeTarget;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <WRITE_TARGET> WRITE_TARGET getWriteTarget() {
		return (WRITE_TARGET) this.writeTarget;
	}
}
