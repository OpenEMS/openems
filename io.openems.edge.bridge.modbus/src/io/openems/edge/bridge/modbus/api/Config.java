package io.openems.edge.bridge.modbus.api;

import java.util.function.Supplier;

import org.slf4j.Logger;

public class Config {

	public final String id;
	public final String alias;
	public final boolean enabled;
	public final int invalidateElementsAfterReadErrors;
	public final LogHandler log;

	public Config(String id, String alias, boolean enabled, LogVerbosity logVerbosity,
			int invalidateElementsAfterReadErrors) {
		this.id = id;
		this.alias = alias;
		this.enabled = enabled;
		this.invalidateElementsAfterReadErrors = invalidateElementsAfterReadErrors;
		this.log = new LogHandler(this, logVerbosity);
	}

	public static class LogHandler {
		public final LogVerbosity verbosity;

		private final Config config;

		private LogHandler(Config config, LogVerbosity logVerbosity) {
			this.config = config;
			this.verbosity = logVerbosity;
		}

		/**
		 * Logs messages for
		 * {@link LogVerbosity#READS_AND_WRITES_DURATION_TRACE_EVENTS}.
		 * 
		 * @param logger  the {@link Logger}
		 * @param message the String message
		 */
		public void trace(Logger logger, Supplier<String> message) {
			if (this.isTrace()) {
				logger.info("[" + this.config.id + "] " + message.get());
			}
		}

		/**
		 * Return true if {@link LogVerbosity#READS_AND_WRITES_DURATION_TRACE_EVENTS} is
		 * active.
		 * 
		 * @return true for trace-log
		 */
		public boolean isTrace() {
			return switch (this.verbosity) {
			case NONE, DEBUG_LOG, READS_AND_WRITES, READS_AND_WRITES_DURATION, READS_AND_WRITES_VERBOSE -> false;
			case READS_AND_WRITES_DURATION_TRACE_EVENTS -> true;
			};
		}
	}
}