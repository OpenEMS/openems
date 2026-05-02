package io.openems.edge.heat.askoma;

import io.openems.common.types.OptionsEnum;

/**
 * Operating mode as reported via the OpenEMS channel. Contains an explicit
 * {@link #UNDEFINED} state so that "not yet known" can be distinguished from
 * {@link #OFF}. For the config-only representation (without {@code UNDEFINED})
 * see {@link Mode}.
 */
public enum ChannelMode implements OptionsEnum {
	UNDEFINED(-1, "UNDEFINED"), //
	OFF(1, "OFF"), //
	FAST_HEAT(2, "FAST_HEAT"), //
	SURPLUS(3, "SURPLUS"); //

	private final int value;
	private final String name;

	ChannelMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	/**
	 * Mapping method from Mode to ChannelMode. Instead of valueOf use this factory
	 * method.
	 * 
	 * @param mode Mode from config
	 * @return ChannelMode
	 */
	public static ChannelMode fromMode(Mode mode) {
		return switch (mode) {
		case OFF -> OFF;
		case FAST_HEAT -> FAST_HEAT;
		case SURPLUS -> SURPLUS;
		};
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
