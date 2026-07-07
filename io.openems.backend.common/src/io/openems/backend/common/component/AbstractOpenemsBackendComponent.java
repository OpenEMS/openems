package io.openems.backend.common.component;

import java.util.Objects;

import org.slf4j.Logger;

import io.openems.common.logger.LazyContextLogger;

// TODO merge with OpenemsComponent of Edge
public class AbstractOpenemsBackendComponent {
	
	private static final String LOG_FORMAT = "[{}] {}";

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
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logInfo(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.info(LOG_FORMAT, component.getName(), message);
		} else {
			log.info(message);
		}
	}

	/**
	 * Log an info message including the Component ID.
	 * 
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
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
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logWarn(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.warn(LOG_FORMAT, component.getName(), message);
		} else {
			log.warn(message);
		}
	}

	/**
	 * Log a warn message including the Component ID.
	 * 
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
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
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logError(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.error(LOG_FORMAT, component.getName(), message);
		} else {
			log.error(message);
		}
	}

	/**
	 * Log an error message including the Component ID.
	 * 
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
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
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
	 *
	 * @param component the {@link AbstractOpenemsBackendComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logDebug(AbstractOpenemsBackendComponent component, Logger log, String message) {
		if (component != null) {
			log.debug(LOG_FORMAT, component.getName(), message);
		} else {
			log.debug(message);
		}
	}

	/**
	 * Log a debug message including the Component ID.
	 * 
	 * <p>
	 * <strong>DEPRECATED</strong>: Use {@link #getComponentLogger(AbstractOpenemsBackendComponent)}
	 * to create a Logger that automatically includes the component name in all log
	 * messages, and then use that Logger for logging instead of this method.
	 *
	 * @param log     the Logger that is used for writing the log
	 * @param message the Debug-message
	 */
	protected void logDebug(Logger log, String message) {
		AbstractOpenemsBackendComponent.logDebug(this, log, message);
	}

	/**
	 * Creates a Logger for the given component, that prefixes all log messages with
	 * the component's name.
	 *
	 * @param component the component for which the Logger should be created
	 * @return a Logger instance
	 */
	public static Logger getComponentLogger(AbstractOpenemsBackendComponent component) {
		Objects.requireNonNull(component, "component is null");
		return new LazyContextLogger(component.getClass(), component::getName);
	}

}
