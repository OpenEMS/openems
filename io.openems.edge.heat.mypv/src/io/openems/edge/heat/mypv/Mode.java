package io.openems.edge.heat.mypv;

import io.openems.common.types.OptionsEnum;

/**
 * Operating mode as configured via OSGi/Config. For the channel representation
 * (which additionally carries an {@code UNDEFINED} state) see
 * {@link ChannelMode}.
 */
public enum Mode implements OptionsEnum {
	OFF(1, "OFF"), //
	FAST_HEAT(2, "FAST_HEAT"), //
	SURPLUS(3, "SURPLUS"); //

	private final int value;
	private final String name;

	Mode(int value, String name) {
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
		return OFF;
	}
}
