package io.openems.edge.evcs.test;

import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.evcs.api.EvcsPower;

public class DummyEvcsPower implements EvcsPower {

	private final RampFilter rampFilter;

	public DummyEvcsPower(RampFilter rampFilter) {
		this.rampFilter = rampFilter;
	}

	public DummyEvcsPower() {
		this(new RampFilter());
	}

	@Override
	public RampFilter getRampFilter() {
		return this.rampFilter;
	}

	@Override
	public float getIncreaseRate() {
		return 0.05f;
	}

}
