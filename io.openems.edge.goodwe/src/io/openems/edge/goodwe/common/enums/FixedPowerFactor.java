package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum FixedPowerFactor implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	LAGGING_0_99(1, "0.99 lagging"), //
	LAGGING_0_98(2, "0.98 lagging"), //
	LAGGING_0_97(3, "0.97 lagging"), //
	LAGGING_0_96(4, "0.96 lagging"), //
	LAGGING_0_95(5, "0.95 lagging"), //
	LAGGING_0_94(6, "0.94 lagging"), //
	LAGGING_0_93(7, "0.93 lagging"), //
	LAGGING_0_92(8, "0.92 lagging"), //
	LAGGING_0_91(9, "0.91 lagging"), //
	LAGGING_0_90(10, "0.90 lagging"), //
	LAGGING_0_89(11, "0.89 lagging"), //
	LAGGING_0_88(12, "0.88 lagging"), //
	LAGGING_0_87(13, "0.87 lagging"), //
	LAGGING_0_86(14, "0.86 lagging"), //
	LAGGING_0_85(15, "0.85 lagging"), //
	LAGGING_0_84(16, "0.84 lagging"), //
	LAGGING_0_83(17, "0.83 lagging"), //
	LAGGING_0_82(18, "0.82 lagging"), //
	LAGGING_0_81(19, "0.81 lagging"), //
	LAGGING_0_80(20, "0.80 lagging"), //
	LEADING_0_80(80, "0.80 leading"), //
	LEADING_0_81(81, "0.81 leading"), //
	LEADING_0_82(82, "0.82 leading"), //
	LEADING_0_83(83, "0.83 leading"), //
	LEADING_0_84(84, "0.84 leading"), //
	LEADING_0_85(85, "0.85 leading"), //
	LEADING_0_86(86, "0.86 leading"), //
	LEADING_0_87(87, "0.87 leading"), //
	LEADING_0_88(88, "0.88 leading"), //
	LEADING_0_89(89, "0.89 leading"), //
	LEADING_0_90(90, "0.90 leading"), //
	LEADING_0_91(91, "0.91 leading"), //
	LEADING_0_92(92, "0.92 leading"), //
	LEADING_0_93(93, "0.93 leading"), //
	LEADING_0_94(94, "0.94 leading"), //
	LEADING_0_95(95, "0.95 leading"), //
	LEADING_0_96(96, "0.96 leading"), //
	LEADING_0_97(97, "0.97 leading"), //
	LEADING_0_98(98, "0.98 leading"), //
	LEADING_0_99(99, "0.99 leading"), //
	LEADING_1(100, "1 leading");

	private final int value;
	private final String option;

	private FixedPowerFactor(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}