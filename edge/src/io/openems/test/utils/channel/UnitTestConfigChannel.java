package io.openems.test.utils.channel;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.thing.Thing;

public class UnitTestConfigChannel<T> extends ConfigChannel<T> {

	public UnitTestConfigChannel(String id, Thing parent) {
		super(id, parent);
	}

	public void setValue(T value) {
		this.updateValue(value);
	}

}
