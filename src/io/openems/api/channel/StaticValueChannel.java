package io.openems.api.channel;

import io.openems.api.thing.Thing;

public class StaticValueChannel<T> extends ReadChannel<T> {

	public StaticValueChannel(String id, Thing parent, T value) {
		super(id, parent);
		this.updateValue(value);
	}

	@Override public StaticValueChannel<T> unit(String unit) {
		super.unit(unit);
		return this;
	}

	@Override public StaticValueChannel<T> multiplier(Long multiplier) {
		super.multiplier(multiplier);
		return this;
	}

	@Override public StaticValueChannel<T> delta(Long delta) {
		super.delta(delta);
		return this;
	}

	@Override public StaticValueChannel<T> required() {
		return this;
	}

	@Override public StaticValueChannel<T> interval(T min, T max) {
		super.interval(min, max);
		return this;
	}

	@Override public StaticValueChannel<T> updateListener(ChannelUpdateListener... listeners) {
		super.updateListener(listeners);
		return this;
	}

	@Override public StaticValueChannel<T> changeListener(ChannelChangeListener... listeners) {
		super.changeListener(listeners);
		return this;
	}

}
