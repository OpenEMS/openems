package eu.chargetime.ocpp.utilities;

/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Modified by Evgeny Pakhomov <eugene.pakhomov@ubitricity.com>
 *
 * Changes:
 *  * Cut Guava specific annotations
 *  * Ticker dependency removed and abstraction and default implementation moved to this class
 *  * Platform dependency removed and formatCompact4Digits method moved to this class
 *  * Preconditions dependency removed and checkState method moved to this class
 *  * References to Guava versions in methods JavaDoc are cut as it won't be relevant
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Stopwatch implementation. Cut version from Guava (needed to minimize
 * dependency tree for client).
 */
public final class Stopwatch {
	private boolean isRunning;
	private long elapsedNanos;
	private long startTick;
	private Ticker ticker;

	private Stopwatch() {
		this(System::nanoTime);
	}

	public Stopwatch(Ticker ticker) {
		this.ticker = ticker;
	}

	public interface Ticker {
		long read();
	}

	/**
	 * Creates (but does not start) a new stopwatch using {@link System#nanoTime} as
	 * its time source.
	 * 
	 * @return Stopwatch
	 */
	public static Stopwatch createUnstarted() {
		return new Stopwatch();
	}

	/**
	 * Creates (and starts) a new stopwatch using {@link System#nanoTime} as its
	 * time source.
	 * 
	 * @return Stopwatch
	 */
	public static Stopwatch createStarted() {
		return new Stopwatch().start();
	}

	/**
	 * Returns {@code true} if {@link #start()} has been called on this stopwatch,
	 * and {@link #stop()} has not been called since the last call to
	 * {@code start()}.
	 * 
	 * @return is running
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Starts the stopwatch.
	 *
	 * @return this {@code Stopwatch} instance
	 * @throws IllegalStateException if the stopwatch is already running.
	 */
	public Stopwatch start() {
		checkState(false);

		isRunning = true;
		startTick = ticker.read();
		return this;
	}

	/**
	 * Stops the stopwatch. Future reads will return the fixed duration that had
	 * elapsed up to this point.
	 *
	 * @return this {@code Stopwatch} instance
	 * @throws IllegalStateException if the stopwatch is already stopped.
	 */
	public Stopwatch stop() {
		checkState(true);

		long tick = ticker.read();
		isRunning = false;
		elapsedNanos += tick - startTick;
		return this;
	}

	/**
	 * Sets the elapsed time for this stopwatch to zero, and places it in a stopped
	 * state.
	 *
	 * @return this {@code Stopwatch} instance
	 */
	public Stopwatch reset() {
		elapsedNanos = 0;
		isRunning = false;
		return this;
	}

	private long elapsedNanos() {
		return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
	}

	/**
	 * Returns the current elapsed time shown on this stopwatch, expressed in the
	 * desired time unit, with any fraction rounded down.
	 *
	 * <p>
	 * <b>Note:</b> the overhead of measurement can be more than a microsecond, so
	 * it is generally not useful to specify {@link TimeUnit#NANOSECONDS} precision
	 * here.
	 *
	 * <p>
	 * It is generally not a good idea to use an ambiguous, unitless {@code long} to
	 * represent elapsed time. Therefore, we recommend using {@link #elapsed()}
	 * instead, which returns a strongly-typed {@link Duration} instance.
	 * 
	 * @param desiredUnit desired unit.
	 * @return long elapsed
	 */
	public long elapsed(TimeUnit desiredUnit) {
		return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
	}

	/**
	 * Returns the current elapsed time shown on this stopwatch as a
	 * {@link Duration}. Unlike {@link #elapsed(TimeUnit)}, this method does not
	 * lose any precision due to rounding.
	 * 
	 * @return long elapsed
	 */
	public Duration elapsed() {
		return Duration.ofNanos(elapsedNanos());
	}

	/** Returns a string representation of the current elapsed time. */
	@Override
	public String toString() {
		long nanos = elapsedNanos();

		TimeUnit unit = chooseUnit(nanos);
		double value = (double) nanos / NANOSECONDS.convert(1, unit);

		return formatCompact4Digits(value) + " " + abbreviate(unit);
	}

	private void checkState(boolean stateExpectation) {
		if (isRunning != stateExpectation) {
			throw new IllegalStateException("This stopwatch is already " + (isRunning ? "running" : "stopped"));
		}
		;
	}

	private static TimeUnit chooseUnit(long nanos) {
		if (DAYS.convert(nanos, NANOSECONDS) > 0) {
			return DAYS;
		}
		if (HOURS.convert(nanos, NANOSECONDS) > 0) {
			return HOURS;
		}
		if (MINUTES.convert(nanos, NANOSECONDS) > 0) {
			return MINUTES;
		}
		if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
			return SECONDS;
		}
		if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
			return MILLISECONDS;
		}
		if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
			return MICROSECONDS;
		}
		return NANOSECONDS;
	}

	private static String abbreviate(TimeUnit unit) {
		switch (unit) {
		case NANOSECONDS:
			return "ns";
		case MICROSECONDS:
			return "\u03bcs"; // μs
		case MILLISECONDS:
			return "ms";
		case SECONDS:
			return "s";
		case MINUTES:
			return "min";
		case HOURS:
			return "h";
		case DAYS:
			return "d";
		default:
			throw new AssertionError();
		}
	}

	private static String formatCompact4Digits(double value) {
		return String.format(Locale.ROOT, "%.4g", value);
	}
}
