package io.openems.edge.energy.api.simulation.periods;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class Periods {

	private final ImmutableList<Period> periods;

	private Periods(ImmutableList<Period> periods) {
		this.periods = periods;
	}

	/**
	 * Creates a validated immutable sequence of periods.
	 *
	 * <p>
	 * The following invariants are guaranteed:
	 * <ul>
	 * <li>{@link Period#time() timestamps} are unique (no duplicates)</li>
	 * <li>{@link Period#time() timestamps} are strictly consecutive based on each
	 * period's duration</li>
	 * <li>{@link Period#data() period data structure} has consistent value presence
	 * across all entries</li>
	 * </ul>
	 *
	 * @param periods the list of periods to wrap
	 * @return a validated {@link Periods} instance
	 * @throws InvalidPeriodsException if the provided periods are null or violate
	 *                                 required invariants
	 */
	public static Periods of(ImmutableList<Period> periods) {
		validate(periods);
		return new Periods(periods);
	}

	/**
	 * Gets the number of {@link Period Periods}.
	 *
	 * @return size
	 */
	public int size() {
		return this.periods.size();
	}

	/**
	 * Are there any {@link Period Periods}?.
	 *
	 * @return true if none
	 */
	public boolean isEmpty() {
		return this.periods.isEmpty();
	}

	/**
	 * Gets a Stream of {@link Period Periods}.
	 *
	 * @return {@link Stream}
	 */
	public Stream<Period> stream() {
		return this.periods.stream();
	}

	/**
	 * Gets the {@link Period} with given index.
	 *
	 * @param index the index
	 * @return the {@link Period}
	 */
	public Period get(int index) {
		return this.periods.get(index);
	}

	/**
	 * Gets the first {@link Period}.
	 *
	 * @return the {@link Period}
	 */
	public Period getFirst() throws NoSuchElementException {
		return this.periods.getFirst();
	}

	/**
	 * Gets the last {@link Period}.
	 *
	 * @return the {@link Period}
	 */
	public Period getLast() throws NoSuchElementException {
		return this.periods.getLast();
	}

	/**
	 * Returns a builder for {@link Periods}.
	 *
	 * @param environment the {@link Environment}
	 * @return a {@link PeriodsBuilder}
	 */
	public static PeriodsBuilder builder(Environment environment) {
		return new PeriodsBuilder(environment);
	}

	/**
	 * Gets object with no {@link Period Periods}.
	 *
	 * @return empty {@link Periods}
	 */
	public static Periods empty() {
		return new Periods(ImmutableList.of());
	}

	/**
	 * Copies the Quarterly Periods of the given {@link Periods} to a new Periods.
	 *
	 * @param o the given Periods
	 * @return copy
	 */
	public static Periods copyOfQuarterly(Periods o) {
		return new Periods(o.stream() //
				.flatMap(period -> switch (period) {
				case Period.Hour ph //
					-> ph.quarterPeriods().stream();
				case Period.Quarter pq //
					-> Stream.of(pq);
				}) //
				.collect(ImmutableList.<Period>toImmutableList()));
	}

	private static void validate(ImmutableList<Period> periods) {
		Objects.requireNonNull(periods, "Periods must not be null");

		if (periods.isEmpty()) {
			return;
		}

		validateTimestamps(periods);
		validateDataConsistency(periods);
	}

	private static void validateTimestamps(ImmutableList<Period> periods) {
		final var seenTimestamps = new HashSet<ZonedDateTime>();

		Period previous = null;
		for (var p : periods) {
			if (!seenTimestamps.add(p.time())) {
				throw new InvalidPeriodsException("Duplicate timestamp: " + p.time());
			}

			if (previous != null) {
				final var expectedTime = previous.time().plus(previous.duration().duration);

				if (!p.time().equals(expectedTime)) {
					throw new InvalidPeriodsException(
							"Invalid timestamp sequence. Expected " + expectedTime + " but was " + p.time());
				}
			}

			previous = p;
		}
	}

	private static void validateDataConsistency(ImmutableList<Period> periods) {
		final var firstPeriodData = periods.getFirst().data();

		for (int i = 1; i < periods.size(); i++) {
			final var currentPeriodData = periods.get(i).data();

			if (!currentPeriodData.hasConsistentValuePresence(firstPeriodData)) {
				throw new InvalidPeriodsException(//
						"Inconsistent period data at index " + i //
								+ "\ncurrent  = " + currentPeriodData //
								+ "\nexpected = " + firstPeriodData);
			}
		}
	}

	public static class InvalidPeriodsException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InvalidPeriodsException(String message) {
			super(message);
		}
	}
}
