package io.openems.test.utils.channel;

import io.openems.api.channel.WriteChannel;
import io.openems.api.thing.Thing;

public class UnitTestWriteChannel<T> extends WriteChannel<T> {

	public UnitTestWriteChannel(String id, Thing parent) {
		super(id, parent);
	}

	public void setValue(T value) {
		this.updateValue(value);
	}

}
