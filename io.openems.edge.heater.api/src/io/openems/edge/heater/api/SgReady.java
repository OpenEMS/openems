package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;

public enum SgReady implements OptionsEnum {

    /**
     * Unknown state.
     */
    UNDEFINED(-1, "Undefined"), //

    /**
     * The Lock state is downward compatible with the energy provider block that is
     * frequently activated at specific times and consists of a maximum 'hard'.
     */
    LOCK(0, "Blocks everything till an internal maximum time of default two hours"), //

    /**
     * The heat pump runs in energy-efficient standard operation with proportional
     * filling of the heat storage tank for the maximum energy provider blocking
     * period of two hours.
     */
    REGULAR(1, "Default energy-efficient operation"), //

    /**
     * The heat pump runs in a more sufficient mode for space heating and hot water
     * production, to use available surplus power.
     */
    RECOMMENDATION(2, "Recommendation to use more available power"), //

    /**
     * The heat pump runs in a definitive start/heat-up mode. Depending on the heat
     * pump, heating is forced and additional heaters may be switched on.
     */
    FORCE_ON(3, "Force all possible consumption of the heat pump"); //

    private final int value;
    private final String name;

    private SgReady(int value, String name) {
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
	return SgReady.UNDEFINED;
    }
}
