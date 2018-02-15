package io.openems.api.channel;

import io.openems.api.thing.Thing;

public class ThingStateChannel extends ReadChannel<Boolean> {

	public ThingStateChannel(String id, Thing parent) {
		super(id, parent);
	}

}
