package io.openems.edge.scheduler.api;

import java.util.Map;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractScheduler extends AbstractOpenemsComponent implements Scheduler {

	private int cycleTime = Scheduler.DEFAULT_CYCLE_TIME;

	protected void activate(ComponentContext context, Map<String, Object> properties, String id, boolean enabled, int cycleTime) {
		if (cycleTime < 1) {
			this.cycleTime = Scheduler.DEFAULT_CYCLE_TIME;
		} else {
			this.cycleTime = cycleTime;
		}
		super.activate(context, properties, id, enabled);
	}

	@Override
	protected void activate(ComponentContext context, Map<String, Object> properties,  String id, boolean enabled) {
		this.activate(context, properties, id, enabled, Scheduler.DEFAULT_CYCLE_TIME);
	}

	@Override
	public int getCycleTime() {
		return cycleTime;
	}
}
