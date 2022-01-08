package io.openems.backend.common.component;

import org.slf4j.Logger;

// TODO merge with OpenemsComponent of Edge
public class AbstractOpenemsBackendComponent {

	private final String name;

	/**
	 * Initializes the AbstractOpenemsBackendComponent.
	 *
	 * @param name a descriptive name for this component. Available via
	 *             {@link #getName()}
	 */
	public AbstractOpenemsBackendComponent(String name) {
		this.name = name;
	}

	/**
	 * A descriptive name for this component.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Log an info message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Info-message
	 */
	protected void logInfo(Logger log, String message) {
		log.info("[" + this.getName() + "] " + message);
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Warn-message
	 */
	protected void logWarn(Logger log, String message) {
		log.warn("[" + this.getName() + "] " + message);
	}

	/**
	 * Log an error message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Error-message
	 */
	protected void logError(Logger log, String message) {
		log.error("[" + this.getName() + "] " + message);
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Debug-message
	 */
	protected void logDebug(Logger log, String message) {
		log.debug("[" + this.getName() + "] " + message);
	}

}
