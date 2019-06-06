package io.openems.edge.scheduler.api;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractScheduler extends AbstractOpenemsComponent implements Scheduler {

	private int cycleTime = Scheduler.DEFAULT_CYCLE_TIME;

	protected AbstractScheduler(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int cycleTime) {
		if (cycleTime < 1) {
			this.cycleTime = Scheduler.DEFAULT_CYCLE_TIME;
		} else {
			this.cycleTime = cycleTime;
		}
		super.activate(context, id, alias, enabled);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		this.activate(context, id, alias, enabled, Scheduler.DEFAULT_CYCLE_TIME);
	}

	@Override
	public int getCycleTime() {
		return cycleTime;
	}
}
