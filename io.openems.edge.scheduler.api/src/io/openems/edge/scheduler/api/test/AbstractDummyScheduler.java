package io.openems.edge.scheduler.api.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.scheduler.api.Scheduler;

public abstract class AbstractDummyScheduler<SELF extends AbstractDummyScheduler<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements Scheduler, OpenemsComponent {

	protected AbstractDummyScheduler(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

}
