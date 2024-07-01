package io.openems.edge.common.statemachine;

import org.slf4j.Logger;

import io.openems.edge.common.component.OpenemsComponent;

public class AbstractContext<PARENT extends OpenemsComponent> {

	private final PARENT parent;

	/**
	 * Constructs an {@link AbstractContext} without useful logging.
	 */
	public AbstractContext() {
		this(null);
	}

	/**
	 * Constructs an {@link AbstractContext}.
	 *
	 * @param parent the parent {@link OpenemsComponent}. This is used to provide
	 *               useful logging.
	 */
	public AbstractContext(PARENT parent) {
		this.parent = parent;
	}

	/**
	 * Gets the parent {@link OpenemsComponent}.
	 *
	 * @return the parent
	 */
	public PARENT getParent() {
		return this.parent;
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	public void logDebug(Logger log, String message) {
		OpenemsComponent.logDebug(this.parent, log, message);
	}

	/**
	 * Log an info message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	public void logInfo(Logger log, String message) {
		OpenemsComponent.logInfo(this.parent, log, message);
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	public void logWarn(Logger log, String message) {
		OpenemsComponent.logWarn(this.parent, log, message);
	}

	/**
	 * Log an error message including the Component ID.
	 *
	 * @param log     the Logger instance
	 * @param message the message
	 */
	public void logError(Logger log, String message) {
		OpenemsComponent.logError(this.parent, log, message);
	}
}
