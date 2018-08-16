package io.openems.core;

import io.openems.api.thing.Thing;

public interface ThingsChangedListener {

	public enum Action {
		ADD, REMOVE
	}

	public void thingChanged(Thing thing, Action action);
}
