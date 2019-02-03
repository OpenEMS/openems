package io.openems.edge.scheduler.api;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractScheduler extends AbstractOpenemsComponent implements Scheduler {

	private int cycleTime = Scheduler.DEFAULT_CYCLE_TIME;

	protected void activate(ComponentContext context, String id, boolean enabled, int cycleTime) {
		if (cycleTime < 1) {
			this.cycleTime = Scheduler.DEFAULT_CYCLE_TIME;
		} else {
			this.cycleTime = cycleTime;
		}
		super.activate(context, id, enabled);
	}

	@Override
	protected void activate(ComponentContext context, String id, boolean enabled) {
		this.activate(context, id, enabled, Scheduler.DEFAULT_CYCLE_TIME);
	}

	@Override
	public int getCycleTime() {
		return cycleTime;
	}
}
