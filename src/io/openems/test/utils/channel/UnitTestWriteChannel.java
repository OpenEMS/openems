package io.openems.test.utils.channel;

import java.util.Optional;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.thing.Thing;

public class UnitTestWriteChannel<T> extends WriteChannel<T> {

	public UnitTestWriteChannel(String deviceName, String id) {
		super(id, new Thing() {

			@Override
			public String id() {
				return id;
			}
		});
	}

	public UnitTestWriteChannel(String id, Thing parent) {
		super(id, parent);
	}

	public void setValue(T value) {
		this.updateValue(value);
	}

	public Optional<T> getWrittenValue() {
		return writeShadowCopy;
	}

	@Override
	public UnitTestWriteChannel<T> maxWriteChannel(ReadChannel<T> channel) {
		return (UnitTestWriteChannel<T>) super.maxWriteChannel(channel);
	}

	@Override
	public UnitTestWriteChannel<T> minWriteChannel(ReadChannel<T> channel) {
		return (UnitTestWriteChannel<T>) super.minWriteChannel(channel);
	}

}
