package io.openems.edge.system.fenecon.home.enums;

public enum Color {
	RED("red"), //
	GREEN("green"), //
	BLUE("blue");

	private final String name;

	private Color(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
