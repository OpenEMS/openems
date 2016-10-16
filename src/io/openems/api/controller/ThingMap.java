package io.openems.api.controller;

public class ThingMap {
	private final String thingId;
	
	public ThingMap(String thingId) {
		this.thingId = thingId;
	}
	
	public String getThingId() {
		return thingId;
	}
}
