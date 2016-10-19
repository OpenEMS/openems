package io.openems.impl.scheduler;

import java.util.Collections;

import io.openems.api.controller.Controller;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.databus.Databus;

public class SimpleScheduler extends Scheduler {
	public SimpleScheduler(Databus databus) {
		super(databus);
	}

	@Override
	public void activate() {
		log.debug("Activate SimpleScheduler");
		super.activate();
	}

	@Override
	protected void dispose() {
	}

	@Override
	protected void forever() {
		Collections.sort(controllers, (c1, c2) -> c2.getPriority() - c1.getPriority());
		for (Controller controller : controllers) {
			// TODO: check if WritableChannels can still be changed, before executing
			controller.run();
		}
		databus.writeAll();
	}

	@Override
	protected boolean initialize() {
		return true;
	}
}
