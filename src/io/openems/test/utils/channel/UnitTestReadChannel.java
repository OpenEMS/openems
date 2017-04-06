package io.openems.test.utils.channel;

import io.openems.api.thing.Thing;

public class UnitTestReadChannel<T> extends io.openems.api.channel.ReadChannel<T> {

	public UnitTestReadChannel(String id, Thing parent) {
		super(id, parent);
	}

	public void setValue(T value) {
		this.updateValue(value);
	}

}
