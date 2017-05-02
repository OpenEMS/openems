package io.openems.api.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;

public class FunctionalWriteChannel<T> extends WriteChannel<T> implements ChannelUpdateListener {

	private List<WriteChannel<T>> channels = new ArrayList<>();
	private FunctionalReadChannelFunction<T> readFunc;
	private FunctionalWriteChannelFunction<T> writeFunc;

	public FunctionalWriteChannel(String id, Thing parent, FunctionalReadChannelFunction<T> function,
			FunctionalWriteChannelFunction<T> writeFunction, WriteChannel<T>... channels) {
		super(id, parent);
		this.channels.addAll(Arrays.asList(channels));
		this.readFunc = function;
		this.writeFunc = writeFunction;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public FunctionalWriteChannel(String id, Thing parent, FunctionalReadChannelFunction<T> function,
			FunctionalWriteChannelFunction<T> writeFunction) {
		super(id, parent);
		this.readFunc = function;
		this.writeFunc = writeFunction;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public void addChannel(WriteChannel<T> channel) {
		synchronized (channels) {
			this.channels.add(channel);
			channel.addUpdateListener(this);
		}
	}

	public void removeChannel(WriteChannel<T> channel) {
		synchronized (this.channels) {
			channel.removeUpdateListener(this);
			this.channels.remove(channel);
		}
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		synchronized (this.channels) {
			WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			updateValue(readFunc.handle(channels));
		}
	}

	@Override
	public void pushWrite(T value) throws WriteChannelException {
		synchronized (this.channels) {
			WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			String label = labels.get(value);
			writeFunc.handle(value, label, channels);
			super.pushWrite(value);
		}
	}

	@Override
	public FunctionalWriteChannel<T> label(T value, String label) {
		super.label(value, label);
		return this;
	}

	@Override
	public FunctionalWriteChannel<T> unit(String unit) {
		super.unit(unit);
		return this;
	}

}
