package io.openems.backend.metadata.api;

public class Device {
	private final int id;
	private String name;
	private String comment;
	private String producttype;

	public Device(int id, String name, String comment, String producttype) {
		this.id = id;
		this.name = name;
		this.comment = comment;
		this.producttype = producttype;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Device [id=" + id + ", name=" + name + ", comment=" + comment + ", producttype=" + producttype + "]";
	}
}
