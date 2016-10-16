package io.openems.api.controller;

import io.openems.api.thing.IsConfigParameter;
import io.openems.api.thing.Thing;

public abstract class Controller implements Thing {
	public final static String THINGID_PREFIX = "_controller";
	private static int instanceCounter = 0;
	private int priority = Integer.MAX_VALUE;
	private final String thingId;

	public Controller() {
		this.thingId = THINGID_PREFIX + instanceCounter++;
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public String getThingId() {
		return thingId;
	}

	@IsConfigParameter("priority")
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
