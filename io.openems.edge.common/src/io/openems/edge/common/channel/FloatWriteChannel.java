package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatWriteChannel extends FloatReadChannel implements WriteChannel<Float> {

	public static class MirrorToDebugChannel implements Consumer<Channel<Float>> {

		private final Logger log = LoggerFactory.getLogger(MirrorToDebugChannel.class);

		private final ChannelId targetChannelId;

		public MirrorToDebugChannel(ChannelId targetChannelId) {
			this.targetChannelId = targetChannelId;
		}

		@Override
		public void accept(Channel<Float> channel) {
			if (!(channel instanceof FloatWriteChannel)) {
				this.log.error("Channel [" + channel.address()
						+ "] is not an FloatWriteChannel! Unable to register \"onSetNextWrite\"-Listener!");
				return;
			}

			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((FloatWriteChannel) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(this.targetChannelId).setNextValue(value);
			});
		}
	}

	protected FloatWriteChannel(OpenemsComponent component, ChannelId channelId, FloatDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Float> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Float value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Float> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Float, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Float, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}

	/**
	 * An object that holds information about the write target of this Channel, i.e.
	 * a Modbus Register or REST-Api endpoint address. Defaults to null.
	 */
	private Object writeTarget = null;

	@Override
	public <WRITE_TARGET> void setWriteTarget(WRITE_TARGET writeTarget) throws IllegalArgumentException {
		this.writeTarget = WriteChannel.checkWriteTarget(this, writeTarget, writeTarget);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <WRITE_TARGET> WRITE_TARGET getWriteTarget() {
		return (WRITE_TARGET) this.writeTarget;
	}
}
