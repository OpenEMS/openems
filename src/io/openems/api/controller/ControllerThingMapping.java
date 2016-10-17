package io.openems.api.controller;

import io.openems.api.thing.Thing;

public class ControllerThingMapping {
	private final Controller controller;
	private final Thing thing;
	private final ThingMap thingMap;

	public ControllerThingMapping(Controller controller, Thing thing, ThingMap thingMap) {
		this.controller = controller;
		this.thing = thing;
		this.thingMap = thingMap;
	}
}
