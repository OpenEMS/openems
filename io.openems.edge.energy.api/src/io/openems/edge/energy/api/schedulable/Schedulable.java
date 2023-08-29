package io.openems.edge.energy.api.schedulable;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedule.Handler;

public interface Schedulable extends Controller {

	/**
	 * Gets the {@link Handler} for a {@link Schedulable} {@link Controller}.
	 * 
	 * @return the ScheduleHandler
	 */
	public Schedule.Handler<?, ?, ?> getScheduleHandler();

}
