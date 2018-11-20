package io.openems.edge.common.channel.doc;

/**
 * Severity/visibility Level
 */
public enum Level implements OptionsEnum {
	OK(0, "Ok"), //
	INFO(1, "Info"), //
	WARNING(2, "Warning"), //
	FAULT(3, "Fault");

	private final int value;
	private final String name;

	private Level(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return OK;
	}
}