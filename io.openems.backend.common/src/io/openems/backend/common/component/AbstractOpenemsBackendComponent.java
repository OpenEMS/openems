package io.openems.backend.common.component;

import org.slf4j.Logger;

// TODO merge with OpenemsComponent of Edge
public class AbstractOpenemsBackendComponent {

	private final String id;

	public AbstractOpenemsBackendComponent(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	/**
	 * Log an info message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected void logInfo(Logger log, String message) {
		log.info("[" + this.id() + "] " + message);
	}

	/**
	 * Log a warn message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected void logWarn(Logger log, String message) {
		log.warn("[" + this.id() + "] " + message);
	}

	/**
	 * Log an error message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected void logError(Logger log, String message) {
		log.error("[" + this.id() + "] " + message);
	}

}
