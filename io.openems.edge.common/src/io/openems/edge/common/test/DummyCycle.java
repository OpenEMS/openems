package io.openems.edge.common.test;

import io.openems.edge.common.cycle.Cycle;

/**
 * Simulates a Cycle for the OpenEMS Component test framework.
 */
public class DummyCycle implements Cycle {

	private final int cycleTime;

	public DummyCycle(int cycleTime) {
		this.cycleTime = cycleTime;
	}

	@Override
	public int getCycleTime() {
		return this.cycleTime;
	}

}