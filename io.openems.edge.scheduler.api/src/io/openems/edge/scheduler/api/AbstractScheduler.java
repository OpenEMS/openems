package io.openems.edge.scheduler.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractScheduler extends AbstractOpenemsComponent implements Scheduler {

	private int cycleTime = Scheduler.DEFAULT_CYCLE_TIME;

	protected void activate(String id, boolean isEnabled, int cycleTime) {
		if (cycleTime < 1) {
			this.cycleTime = Scheduler.DEFAULT_CYCLE_TIME;
		} else {
			this.cycleTime = cycleTime;
		}
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
