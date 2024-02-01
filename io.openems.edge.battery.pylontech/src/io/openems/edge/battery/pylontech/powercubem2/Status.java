package io.openems.edge.battery.pylontech.powercubem2;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SLEEP(0, "Sleep"), //
	CHARGE(1, "Charge"), //
	DISCHARGE(2, "Discharge"), //
	IDLE(3, "Idle");

	private final int value;
	private final String name;

	private Status(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Returns a Status enum for a particular int value.
	 * 
	 * @param value (int value that represents a Status
	 * @return Status enum for the value
	 */
	public static Status valueOf(int value) {
		for (Status status : Status.values()) {
			if (status.value == value) {
				return status;
			}
		}
		return UNDEFINED;

	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
