package io.openems.edge.controller.ess.fastfrequencyreserve.enums;

//CHECKSTYLE:OFF
public enum ActivationTime {

	SHORT_ACTIVATION_RUN(700, "Short activation time run, in milliseconds"), //
	MEDIUM_ACTIVATION_RUN(1000, "Medium activation time run, in milliseconds"), //
	LONG_ACTIVATION_RUN(1300, "Long activation time run, in milliseconds");

	private final long value;
	private final String name;

	private ActivationTime(long value, String name) {
		this.value = value;
		this.name = name;
	}

	public long getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

}
//CHECKSTYLE:ON