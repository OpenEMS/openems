package io.openems.femsserver.femswebsocket;

public class Device {
	private final String name;
	private final String comment;
	
	public Device(String name, String comment) {
		this.name = name;
		this.comment = comment;
	}

	@Override
	public String toString() {
		return "Device [name=" + name + ", comment=" + comment + "]";
	}
	
}
