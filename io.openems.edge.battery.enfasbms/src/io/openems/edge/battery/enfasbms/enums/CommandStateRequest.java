package io.openems.edge.battery.enfasbms.enums;

import io.openems.common.types.OptionsEnum;

public enum CommandStateRequest implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	COMMAND_DO_NOTHING(0, "Command Do nothing"), //
	COMMAND_CLOSE_CONTACTORS(10, "Command Close contactors"), //
	COMMAND_OPEN_CONTACTORS(12, "Command open contactors"), //
	COMMAND_SLEEP_MODE_REQUEST(14, "Command Sleep mode request");

	private final int value;
	private final String name;

	private CommandStateRequest(int value, String name) {
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

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
