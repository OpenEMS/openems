package io.openems.edge.scheduler.api;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@ProviderType
public interface Scheduler extends OpenemsComponent {

	public final static int DEFAULT_CYCLE_TIME = 1000;

	/**
	 * Returns controllers ordered by their priority.
	 * 
	 * @return
	 */
	public List<Controller> getControllers();

	/**
	 * Returns the Schedulers Cycle-Time in milliseconds. This is the period after
	 * which the Controllers are executed.
	 */
	public int getCycleTime();
}
