package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum WorkWeek implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ECO_MODE_ENABLE(0xFF, "Eco mode enable"), //
	ECO_MODE_DISABLE(0x00, "Eco mode disable"), //
	DRY_CONTACT_LOAD_MODE_ENABLE(0xFE, "Dry contact load mode enable"), //
	DRY_CONTACT_LOAD_MODE_DISABLE(0x01, " Dry contact load mode disable"), //
	DRY_CONTACT_SMART_LOAD_MODE_ENABLE(0xFD, "Dry contact smart load mode enable"), //
	DRY_CONTACT_SMART_LOAD_MODE_DISABLE(0x02, "Dry contact smart load mode disable"), //
	PEAKSHAVING_FUNCTION_ENABLE(0xFC, "Peak shaving function enable"), //
	PEAKSHAVING_FUNCTION_DISABLE(0x03, "Peak shaving function disable"), //
	BACKUP_MODE_ENABLE(0xFB, "Backup mode enable"), //
	BACKUP_MODE_DISABLE(0x04, "Backup mode disable"), //
	; //

	private final int value;
	private final String option;

	private WorkWeek(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}