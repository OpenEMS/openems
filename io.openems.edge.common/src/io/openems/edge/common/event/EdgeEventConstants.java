package io.openems.edge.common.event;

import io.openems.common.types.EdgeConfig;

public final class EdgeEventConstants {

	private EdgeEventConstants() {
		// avoid inheritance
	}

	public static final String TOPIC_BASE = "io/openems/edge/";

	/**
	 * Base for CYCLE events. See @{link CycleWorker} for implementation details.
	 */
	public static final String TOPIC_CYCLE = "io/openems/edge/cycle/";

	/**
	 * BEFORE_PROCESS_IMAGE event
	 *
	 * <p>
	 * allows to execute anything that is required to be executed before the current
	 * processimage is built - i.e. channel.nextProcessImage() is called. The event
	 * is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_BEFORE_PROCESS_IMAGE = TOPIC_CYCLE + "BEFORE_PROCESS_IMAGE";

	/**
	 * AFTER_PROCESS_IMAGE event
	 *
	 * <p>
	 * allows to execute anything that is required to be executed after the current
	 * processimage is built - i.e. channel.nextProcessImage() was called. The event
	 * is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_AFTER_PROCESS_IMAGE = TOPIC_CYCLE + "AFTER_PROCESS_IMAGE";

	/**
	 * BEFORE_CONTROLLERS event
	 *
	 * <p>
	 * allows to execute anything that is required to be executed before the
	 * controllers are executed. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_BEFORE_CONTROLLERS = TOPIC_CYCLE + "BEFORE_CONTROLLERS";

	/**
	 * AFTER_CONTROLLERS event
	 * 
	 * <p>
	 * allows to execute anything that is required to be executed after the
	 * controllers were executed. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_AFTER_CONTROLLERS = TOPIC_CYCLE + "AFTER_CONTROLLERS";

	/**
	 * BEFORE_WRITE event
	 *
	 * <p>
	 * allows to execute anything that is required to be executed before the data is
	 * actually written to the devices. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_BEFORE_WRITE = TOPIC_CYCLE + "BEFORE_WRITE";

	/**
	 * EXECUTE_WRITE event
	 *
	 * <p>
	 * triggers to actually write the data to the devices. The event is executed
	 * synchronously.
	 */
	public static final String TOPIC_CYCLE_EXECUTE_WRITE = TOPIC_CYCLE + "EXECUTE_WRITE";

	/**
	 * AFTER_WRITE event
	 *
	 * <p>
	 * allows to execute anything that is required to be executed after the data was
	 * actually written to the devices. The event is executed synchronously.
	 */
	public static final String TOPIC_CYCLE_AFTER_WRITE = TOPIC_CYCLE + "AFTER_WRITE";

	/**
	 * Base for CONFIG events. See @{link {@link EdgeConfig}} for implementation
	 * details.
	 */
	public static final String TOPIC_CONFIG = "io/openems/edge/config/";

	/**
	 * UPDATE event
	 *
	 * <p>
	 * Called when the {@link EdgeConfig} was updated, e.g. because configuration
	 * properties changed or Channels changed. The EdgeConfig object is sent with
	 * the event - see {@value #TOPIC_CONFIG_UPDATE_KEY}. The event is executed
	 * synchronously.
	 */
	public static final String TOPIC_CONFIG_UPDATE = TOPIC_CONFIG + "UPDATE";

	/**
	 * The key of the {@link EdgeConfig} object in the event attachments map.
	 */
	public static final String TOPIC_CONFIG_UPDATE_KEY = "TOPIC_CONFIG_UPDATE_KEY";
}
