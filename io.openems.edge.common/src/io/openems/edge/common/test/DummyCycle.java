package io.openems.edge.common.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;

/**
 * Simulates a {@link Cycle} for the OpenEMS Component test framework.
 */
public class DummyCycle extends AbstractDummyOpenemsComponent<DummyCycle> implements Cycle {

	private final int cycleTime;

	public DummyCycle(int cycleTime) {
		super("_cycle", //
				OpenemsComponent.ChannelId.values(), //
				Cycle.ChannelId.values() //
		);
		this.cycleTime = cycleTime;
	}

	@Override
	protected DummyCycle self() {
		return this;
	}

	@Override
	public int getCycleTime() {
		return this.cycleTime;
	}

}