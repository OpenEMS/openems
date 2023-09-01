package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GoodWeType implements OptionsEnum {
	UNDEFINED(-1, "Undefined", Series.UNDEFINED), //
	GOODWE_10K_BT(10, "GoodWe GW10K-BT", Series.BT), //
	GOODWE_8K_BT(11, "GoodWe GW8K-BT", Series.BT), //
	GOODWE_5K_BT(12, "GoodWe GW5K-BT", Series.BT), //
	GOODWE_10K_ET(20, "GoodWe GW10K-ET", Series.ET), //
	GOODWE_8K_ET(21, "GoodWe GW8K-ET", Series.ET), //
	GOODWE_5K_ET(22, "GoodWe GW5K-ET", Series.ET), //
	FENECON_FHI_10_DAH(30, "FENECON FHI 10 DAH", Series.ET);

	public static enum Series {
		UNDEFINED, BT, ET;
	}

	private final int value;
	private final String option;
	private final Series series;

	private GoodWeType(int value, String option, Series series) {
		this.value = value;
		this.option = option;
		this.series = series;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	/**
	 * Is this GoodWe a ET-Series or BT-Series.
	 *
	 * @return the Series or UNDEFINED if unknown
	 */
	public Series getSeries() {
		return this.series;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}