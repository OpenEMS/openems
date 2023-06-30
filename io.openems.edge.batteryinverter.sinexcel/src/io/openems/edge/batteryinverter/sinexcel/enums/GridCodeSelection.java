package io.openems.edge.batteryinverter.sinexcel.enums;

public enum GridCodeSelection {
	SA1741("SA1741", 0), //
	VDE("VDE", 1), //
	AUSTRALIAN("Australian", 2), //
	G99("G99", 3), //
	HAWAIIAN("Hawaiian", 4), //
	EN50549("EN50549", 5), //
	AUSTRIA_TYPEA("Austria Type A", 6);//

	private final String name;
	private final int value;

	private GridCodeSelection(String name, int value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public int getValue() {
		return this.value;
	}
}