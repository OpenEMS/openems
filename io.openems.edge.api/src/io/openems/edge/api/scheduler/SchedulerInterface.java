package io.openems.edge.api.scheduler;

import java.util.List;

import io.openems.edge.api.controller.ControllerInterface;


public interface SchedulerInterface {

	
	/**
	 * Returns all Controllers which should be executed at the moment in the appropriate order.
	 * @return
	 */
	List<ControllerInterface> getController();
	
}
