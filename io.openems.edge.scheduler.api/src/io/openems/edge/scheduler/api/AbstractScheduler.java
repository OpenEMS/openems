package io.openems.edge.scheduler.api;

import io.openems.common.types.AbstractOpenemsComponent;

public abstract class AbstractScheduler extends AbstractOpenemsComponent implements Scheduler {

	private int cycleTime = Scheduler.DEFAULT_CYCLE_TIME;

	protected void activate(String id, boolean isEnabled, int cycleTime) {
		this.cycleTime = cycleTime;
		super.activate(id, isEnabled);
	}

	@Override
	protected void activate(String id, boolean isEnabled) {
		this.activate(id, isEnabled, Scheduler.DEFAULT_CYCLE_TIME);
	}

	@Override
	public int getCycleTime() {
		return cycleTime;
	}
}
