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
	 * Log a info message including the Component ID.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logInfo(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.info("[" + component.getName() + "] " + message);
		} else {
			log.info(message);
		}
	}

	/**
	 * Log an info message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Info-message
	 */
	protected void logInfo(Logger log, String message) {
		AbstractOpenemsBackendComponent.logInfo(this, log, message);
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logWarn(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.warn("[" + component.getName() + "] " + message);
		} else {
			log.warn(message);
		}
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Warn-message
	 */
	protected void logWarn(Logger log, String message) {
		AbstractOpenemsBackendComponent.logWarn(this, log, message);
	}

	/**
	 * Log a error message including the Component ID.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logError(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.error("[" + component.getName() + "] " + message);
		} else {
			log.error(message);
		}
	}

	/**
	 * Log an error message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Error-message
	 */
	protected void logError(Logger log, String message) {
		AbstractOpenemsBackendComponent.logError(this, log, message);
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logDebug(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.error("[" + component.getName() + "] " + message);
		} else {
			log.error(message);
		}
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Debug-message
	 */
	protected void logDebug(Logger log, String message) {
		AbstractOpenemsBackendComponent.logDebug(this, log, message);
	}

}
