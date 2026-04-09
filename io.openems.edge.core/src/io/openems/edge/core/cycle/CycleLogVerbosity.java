package io.openems.edge.core.cycle;

public enum CycleLogVerbosity {

	/**
	 * No cycle-related logs.
	 */
	NONE,

	/**
	 * Only high-level cycle summary (measured cycle time, overruns).
	 */
	SUMMARY,

	/**
	 * Log cycle phases (BEFORE/AFTER_PROCESS_IMAGE, CONTROLLERS, WRITE).
	 */
	PHASES,

	/**
	 * Log execution time per Controller.
	 */
	CONTROLLERS,

	/**
	 * Log execution time per Component / Sum / ProcessImage channel update.
	 */
	COMPONENTS,

	/**
	 * Log everything incl. phase timing, controller, components and event tracing.
	 */
	TRACE;

}