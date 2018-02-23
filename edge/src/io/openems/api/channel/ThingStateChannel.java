package io.openems.api.channel;

import io.openems.api.channel.thingstate.ThingStateEnum;
import io.openems.api.thing.Thing;

public class ThingStateChannel extends ReadChannel<Boolean> {

	private final ThingStateEnum state;

	public ThingStateChannel(ThingStateEnum state, Thing parent) {
		super(state.getChannelId(), parent);
		this.state = state;
	}

	public String name() {
		return this.state.toString();
	}
}
