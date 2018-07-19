package io.openems.common.timedata;

public class Tag {
	private final String name;
	private final int value;

	public Tag(String name, int value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}
}
