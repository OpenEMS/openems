package io.openems.edge.common.controllerexecutor;

public final class EdgeEventConstants {

	private EdgeEventConstants() {
		// avoid inheritance
	}

	public static final String TOPIC_BASE = "io/openems/edge/";

	/**
	 * Base for CYCLE events. See @{link ControllerExecutor} for implementation
	 * details.
	 */
	public static final String TOPIC_CYCLE = "io/openems/edge/cycle/";

	/**
	 * BEFORE_WRITE event
	 * 
	 * allows to execute anything that is required to be executed before the data is
	 * actually written to the devices. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_BEFORE_WRITE = TOPIC_CYCLE + "BEFORE_WRITE";

	/**
	 * EXECUTE_WRITE event
	 * 
	 * triggers to actually write the data to the devices. The event is executed
	 * synchronously.
	 */
	public static final String TOPIC_CYCLE_EXECUTE_WRITE = TOPIC_CYCLE + "EXECUTE_WRITE";

	/**
	 * AFTER_WRITE event
	 * 
	 * allows to execute anything that is required to be executed after the data was
	 * actually written to the devices. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_AFTER_WRITE = TOPIC_CYCLE + "AFTER_WRITE";
}
