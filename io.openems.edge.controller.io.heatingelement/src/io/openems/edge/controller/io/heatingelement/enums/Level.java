package io.openems.edge.controller.io.heatingelement.enums;

import io.openems.common.types.OptionsEnum;

public enum Level implements OptionsEnum {
	/**
	 * Switch off everything.
	 */
	LEVEL_0(0, "Switch off everything"),
	/**
	 * When grid-feed-in is more than 2000 W, first phase is switched on.
	 */
	LEVEL_1(1, "Switch-on only first phase"),
	/**
	 * When grid-feed-in is more than 4000 W, first and second phase are switched
	 * on.
	 */
	LEVEL_2(2, "Switch-on first and second phase"),
	/**
	 * When grid-feed-in is more than 4000 W, all phases are switched on.
	 */
	LEVEL_3(3, "Switch-on all the three phase"),
	/**
	 * Fourth Step is switched on.
	 */
	LEVEL_4(4, "Switch-on the fourth step"),
	/**
	 * Fifth Step is switched on.
	 */
	LEVEL_5(5, "Switch-on the fifth step"),
	/**
	 * Sixth Step is switched on.
	 */
	LEVEL_6(6, "Switch-on the sixth step");

	private final int value;
	private final String name;

	private Level(int value, String name) {
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
		return LEVEL_0;
	}

	/**
	 * Gets the {@link Level} with the given value.
	 *
	 * @param value the value, e.g. as returned by {@link #getValue()}
	 * @return the matching Level
	 * @throws IllegalArgumentException if no Level has the given value
	 */
	public static Level fromValue(int value) {
		for (var level : values()) {
			if (level.value == value) {
				return level;
			}
		}
		throw new IllegalArgumentException("No Level with value [" + value + "]");
	}
}
