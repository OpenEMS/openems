package io.openems.edge.energy.api.schedulable;

import com.google.common.collect.ImmutableSet;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedule.Mode;

// CHECKSTYLE:OFF
public interface Schedulable<MODE extends Schedule.Mode> extends Controller {
	// CHECKSTYLE:ON

	/**
	 * Get the available {@link Mode}s.
	 * 
	 * @return set of {@link Mode}s
	 */
	public ImmutableSet<MODE> getAvailableModes();

	/**
	 * Apply a {@link Schedule} of {@link Mode}s.
	 * 
	 * @param schedule the {@link Schedule}
	 */
	public void applySchedule(Schedule<MODE> schedule);

}
