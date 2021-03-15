package io.openems.edge.controller.dynamicbatterycontrol;

import io.openems.common.types.OptionsEnum;

public enum Source implements OptionsEnum {
	API(0, "https://portal.blogpv.net/api/bci/signal"), //
	CSV(1, "BciCsv.csv"); //

	private final int value;
	private final String source;

	private Source(int value, String dataSource) {
		this.value = value;
		this.source = dataSource;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.source;
	}

	@Override
	public OptionsEnum getUndefined() {
		return API;
	}
}
