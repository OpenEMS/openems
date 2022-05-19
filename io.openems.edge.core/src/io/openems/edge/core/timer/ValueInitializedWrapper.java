package io.openems.edge.core.timer;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The ValueInitializedWrapper is a Class that stores the important data the
 * {@link Timer} needs to work. It stores the maxWaitTime (e.g. maxCycles). If
 * the Identifier is initialized or not. The Counter for MaxCycles and the
 * initial DateTime.
 */
public class ValueInitializedWrapper {

	private int maxValue;
	private boolean initialized;
	// only needed by CycleTimer
	private final AtomicInteger counter = new AtomicInteger(0);
	private final AtomicReference<Instant> initialDateTime = new AtomicReference<>();

	public ValueInitializedWrapper(int maxValue, boolean initialized) {
		this.maxValue = maxValue;
		this.initialized = initialized;
		this.initialDateTime.set(Instant.now());
	}

	public ValueInitializedWrapper(int maxValue) {
		this(maxValue, false);
	}

	public int getMaxValue() {
		return this.maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	public boolean isInitialized() {
		return this.initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public AtomicInteger getCounter() {
		return this.counter;
	}

	public AtomicReference<Instant> getInitialDateTime() {
		return this.initialDateTime;
	}

	public void setInitialDateTime(Instant time) {
		this.initialDateTime.set(time);
	}

	public void setCounter(int value) {
		this.counter.set(value);
	}
}
