package io.openems.edge.bridge.modbus.api.worker.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.common.type.TypeUtils;

public class DefectiveComponents {

	public static final int INCREASE_WAIT_SECONDS = 2;
	public static final int MAX_WAIT_SECONDS = 5 * 60; // 5 minutes

	private final Logger log = LoggerFactory.getLogger(DefectiveComponents.class);

	private static record NextTry(Instant timestamp, int count) {
	}

	private final Clock clock;
	private final AtomicReference<LogVerbosity> logVerbosity;
	private final Map<String, NextTry> nextTries = new HashMap<>();

	public DefectiveComponents(AtomicReference<LogVerbosity> logVerbosity) {
		this(Clock.systemDefaultZone(), logVerbosity);
	}

	protected DefectiveComponents() {
		this(Clock.systemDefaultZone(), new AtomicReference<>(LogVerbosity.READS_AND_WRITES_DURATION_TRACE_EVENTS));
	}

	protected DefectiveComponents(Clock clock) {
		this(clock, new AtomicReference<>(LogVerbosity.READS_AND_WRITES_DURATION_TRACE_EVENTS));
	}

	protected DefectiveComponents(Clock clock, AtomicReference<LogVerbosity> logVerbosity) {
		this.clock = clock;
		this.logVerbosity = logVerbosity;
	}

	/**
	 * Adds a defective Component and sets retry time to now() +
	 * {@value #PAUSE_SECONDS}.
	 * 
	 * @param componentId the Component-ID; not null
	 */
	public synchronized void add(String componentId) {
		TypeUtils.assertNull("DefectiveComponents add() takes no null values", componentId);
		this.nextTries.compute(componentId, (k, v) -> {
			var count = (v == null) ? 1 : v.count + 1;
			var wait = Math.min(INCREASE_WAIT_SECONDS * count, MAX_WAIT_SECONDS);
			if (this.isTraceLog()) {
				final String log;
				if (count == 1) {
					log = "Add [" + componentId + "] to defective Components.";
				} else {
					log = "Increase wait for defective Component [" + componentId + "].";
				}
				this.log.info(log + " Wait [" + wait + "s]" + " Count [" + count + "]");
			}
			return new NextTry(Instant.now(this.clock).plusSeconds(wait), count);
		});
	}

	/**
	 * Removes a defective Component.
	 * 
	 * @param componentId the Component-ID; not null
	 */
	public synchronized void remove(String componentId) {
		TypeUtils.assertNull("DefectiveComponents remove() takes no null values", componentId);
		if (this.nextTries.remove(componentId) != null && this.isTraceLog()) {
			this.log.info("Remove [" + componentId + "] from defective Components.");
		}
	}

	/**
	 * Is the given Component known to be defective?.
	 * 
	 * @param componentId the Component-ID
	 * @return true if listed as defective, false if not
	 */
	public synchronized boolean isKnown(String componentId) {
		return this.nextTries.containsKey(componentId);
	}

	/**
	 * Is the given Component due for next try?.
	 * 
	 * @param componentId the Component-ID
	 * @return true if yes, false if no, null if component is not in list of
	 *         defective components
	 */
	public synchronized Boolean isDueForNextTry(String componentId) {
		var nextTry = this.nextTries.get(componentId);
		if (nextTry == null) {
			return null;
		}
		var now = Instant.now(this.clock);
		return now.isAfter(nextTry.timestamp);
	}

	private boolean isTraceLog() {
		return switch (this.logVerbosity.get()) {
		case READS_AND_WRITES, READS_AND_WRITES_DURATION, READS_AND_WRITES_VERBOSE,
				READS_AND_WRITES_DURATION_TRACE_EVENTS ->
			true;
		case NONE, DEBUG_LOG -> false;
		};
	}
}