package io.openems.edge.pump.grundfos;

public enum TpModeSetting {
	NONE(0, "None, not part of a multi pump system"), //
	TIME_ALTERNATING(1, "Time alternating mode"), //
	LOAD_ALTERNATING(2, "Load (power) alternating mode"), //
	CASCADE_CONTROL(3, "Cascade control mode"), //
	BACKUP(4, "Backup mode");

	private int value;
	private String name;

	TpModeSetting(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}
}
