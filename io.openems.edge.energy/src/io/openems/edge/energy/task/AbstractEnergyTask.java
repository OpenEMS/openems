package io.openems.edge.energy.task;

import java.util.function.Consumer;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.energy.api.schedulable.Schedules;

public abstract class AbstractEnergyTask implements Runnable {

	protected final ComponentManager componentManager;
	protected final Consumer<Boolean> scheduleError;

	protected Schedules schedules;

	public AbstractEnergyTask(ComponentManager componentManager, Consumer<Boolean> scheduleError) {
		this.componentManager = componentManager;
		this.scheduleError = scheduleError;
	}

	/**
	 * Gets a debug log message.
	 * 
	 * @return a message
	 */
	public String debugLog() {
		var schedules = this.schedules;
		if (schedules != null) {
			return this.schedules.debugLog();
		} else {
			return "";
		}
	}

}
