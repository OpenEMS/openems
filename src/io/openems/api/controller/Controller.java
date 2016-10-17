package io.openems.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.IsConfigParameter;
import io.openems.api.thing.Thing;

public abstract class Controller implements Thing, Runnable {
	public final static String THINGID_PREFIX = "_controller";
	private static int instanceCounter = 0;
	protected final Logger log;
	private String name;
	private int priority = Integer.MIN_VALUE;

	public Controller() {
		log = LoggerFactory.getLogger(this.getClass());
		name = THINGID_PREFIX + instanceCounter++;
	}

	/**
	 * Returns the priority of this controller. High return value is high priority,
	 * low value is low priority.
	 *
	 * @return
	 */
	public int getPriority() {
		return this.priority;
	};

	@Override
	public String getThingId() {
		return name;
	}

	@IsConfigParameter("priority")
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
