package io.openems.edge.common.channel.doc;

/**
 * Severity/visibility Level
 */
public enum Level implements OptionsEnum {
	/**
	 * "OK" indicates, that everything is OK and there are no messages.
	 */
	OK(0, "Ok"), //
	/**
	 * "Info" indicates, that everything is OK, but there is at least one
	 * informative messages available.
	 */
	INFO(1, "Info"), //
	/**
	 * "Warning" indicates, that there is at least one warning message available.
	 */
	WARNING(2, "Warning"), //
	/**
	 * "Fault" indicates, that there is at least one fault message available.
	 */
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