package io.openems.api.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;

public class FunctionalWriteChannel<T extends Comparable<T>> extends WriteChannel<T> implements ChannelUpdateListener {

	private List<WriteChannel<T>> channels = new ArrayList<>();
	private FunctionalWriteChannelFunction<T> writeValueFunc;

	public FunctionalWriteChannel(String id, Thing parent, FunctionalWriteChannelFunction<T> writeValueFunction,
			@SuppressWarnings("unchecked") WriteChannel<T>... channels) {
		super(id, parent);
		this.channels.addAll(Arrays.asList(channels));
		this.writeValueFunc = writeValueFunction;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public FunctionalWriteChannel(String id, Thing parent, FunctionalWriteChannelFunction<T> writeFunction) {
		super(id, parent);
		this.writeValueFunc = writeFunction;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public void addChannel(WriteChannel<T> channel) {
		synchronized (channels) {
			this.channels.add(channel);
			channel.addUpdateListener(this);
			update();
		}
	}

	public void removeChannel(WriteChannel<T> channel) {
		synchronized (this.channels) {
			channel.removeUpdateListener(this);
			this.channels.remove(channel);
			update();
		}
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		update();
	}

	private void update() {
		synchronized (this.channels) {
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			updateValue(writeValueFunc.getValue(channels));
		}
	}

	@Override
	public void pushWrite(T value) throws WriteChannelException {
		synchronized (this.channels) {
			checkIntervalBoundaries(value);
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			String label = labels.get(value);
			super.pushWrite(writeValueFunc.setValue(value, label, channels));
		}
	}

	@Override
	public void pushWriteMax(T value) throws WriteChannelException {
		synchronized (this.channels) {
			checkIntervalBoundaries(value);
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			String label = labels.get(value);
			super.pushWriteMax(writeValueFunc.setMaxValue(value, label, channels));
		}
	}

	@Override
	public void pushWriteMin(T value) throws WriteChannelException {
		synchronized (this.channels) {
			checkIntervalBoundaries(value);
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			String label = labels.get(value);
			super.pushWriteMin(writeValueFunc.setMinValue(value, label, channels));
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

	@Override
	public Optional<T> writeMin() {
		synchronized (this.channels) {
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			T erg = writeValueFunc.getMinValue(super.writeMin(), channels);
			if (erg != null) {
				return Optional.of(erg);
			}
			return Optional.empty();
		}

	}

	@Override
	public Optional<T> writeMax() {
		synchronized (this.channels) {
			@SuppressWarnings("unchecked") WriteChannel<T>[] channels = new WriteChannel[this.channels.size()];
			this.channels.toArray(channels);
			T erg = writeValueFunc.getMaxValue(super.writeMax(), channels);
			if (erg != null) {
				return Optional.of(erg);
			}
			return Optional.empty();
		}
	}

}
