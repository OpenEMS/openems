package io.openems.edge.energy;

import java.time.ZonedDateTime;

import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.schedulable.Schedules;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 *
 */
public class Task implements Runnable {

	// private final Logger log = LoggerFactory.getLogger(Task.class);

	private final EnergyImpl parent;

	private Schedules schedules;

	public Task(EnergyImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		// Schedules
		var now = ZonedDateTime.now(this.parent.componentManager.getClock());
		var essFixActivePowerModes = Utils.createEssFixActivePowerScheduleFromConfig(this.parent.config);
		var evcsModes = Utils.createEvcsScheduleFromConfig(this.parent.config);

		this.schedules = Schedules.create() //
				.add("ctrlEssFixActivePower0", Schedule.of(now, essFixActivePowerModes)) //
				.add("ctrlEvcs0", Schedule.of(now, evcsModes)) //
				.build();

		this.parent.ctrlFixActivePower0.getScheduleHandler()
				.applySchedule(this.schedules.get("ctrlEssFixActivePower0"));
		this.parent.ctrlEvcs0.getScheduleHandler().applySchedule(this.schedules.get("ctrlEvcs0"));
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
