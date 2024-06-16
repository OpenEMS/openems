package io.openems.edge.bridge.http.time;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;

import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;

public class DelayTimeProviderChain {

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a the {@link Duration}
	 * of zero on request.
	 * 
	 * @return a {@link DelayTimeProviderChain} of zero delay
	 */
	public static DelayTimeProviderChain immediate() {
		return fixedDelay(Duration.ZERO);
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a fixed
	 * {@link Duration} on request.
	 * 
	 * @param duration the {@link Duration} to return when requested
	 * @return a {@link DelayTimeProviderChain} of the given {@link Duration}
	 */
	public static DelayTimeProviderChain fixedDelay(Duration duration) {
		final var delay = Delay.of(duration);
		return new DelayTimeProviderChain(() -> delay);
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a {@link Duration}
	 * till the next truncated time of the given {@link DurationUnit}.
	 * 
	 * <p>
	 * e. g. if a <code>DurationUnit.ofMinutes(1)</code> gets provided the
	 * {@link Duration} at the time 12h 43min 23sec would be (60sec - 23sec) = 37sec
	 * to 12h 44min 0sec. Same would work for every hour 12h, 13h, 14h, ... with
	 * <code>DurationUnit.ofHours(1)</code>
	 * 
	 * @param clock        the {@link Clock} to get the current time
	 * @param durationUnit the {@link DurationUnit} to truncate with
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Duration}
	 */
	public static DelayTimeProviderChain fixedAtEveryFull(Clock clock, DurationUnit durationUnit) {
		return new DelayTimeProviderChain(() -> {
			final var now = LocalDateTime.now(clock);

			return Delay.of(Duration.between(now, now.truncatedTo(durationUnit) //
					.plus(durationUnit.getDuration())));
		});
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a {@link Delay} which
	 * indicates that the next run should never happen. May be used if
	 * {@link Endpoint} credentials are wrong or expired.
	 * 
	 * <p>
	 * NOTE: Do not use a very large {@link Duration} to indicate that a task should
	 * not be executed anymore. An Overflow exception could happen if additional
	 * delay gets added and the task would still be queued but never executed.
	 * 
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay}
	 */
	public static DelayTimeProviderChain runNeverAgain() {
		return new DelayTimeProviderChain(Delay::infinite);
	}

	private final Supplier<Delay> supplier;

	public DelayTimeProviderChain(Supplier<Delay> supplier) {
		this.supplier = supplier;
	}

	public Delay getDelay() {
		return this.supplier.get();
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which adds to the original provider
	 * the given amount. The new provided {@link Duration} gets rounded to seconds
	 * based on the implementation of {@link Delay#plus(Delay)}.
	 * 
	 * @param origin   the original {@link DelayTimeProviderChain} to get the
	 *                 initial {@link Duration}
	 * @param duration the {@link Duration} to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Duration}
	 *         of the original {@link DelayTimeProviderChain} with the added amount
	 */
	public static DelayTimeProviderChain plusFixedAmount(DelayTimeProviderChain origin, Duration duration) {
		return new DelayTimeProviderChain(() -> {
			return origin.getDelay().plus(Delay.of(duration));
		});
	}

	/**
	 * Helper method to create a new {@link DelayTimeProviderChain} with a fixed
	 * amount added.
	 * 
	 * @implNote delegates to
	 *           {@link #plusFixedAmount(DelayTimeProviderChain, int, TimeUnit)}
	 * @param duration the {@link Duration} to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Duration}
	 *         of the original {@link DelayTimeProviderChain} with the added amount
	 * @see #plusFixedAmount(DelayTimeProviderChain, int, TimeUnit)
	 */
	public DelayTimeProviderChain plusFixedAmount(Duration duration) {
		return plusFixedAmount(this, duration);
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which adds to the original provider
	 * a random amount between 0 (inclusive) and bound (exclusive). The new provided
	 * {@link Duration} gets rounded to seconds based on the implementation of
	 * {@link Delay#plus(Delay)}.
	 * 
	 * @param origin the original {@link DelayTimeProviderChain} to get the initial
	 *               {@link Duration}
	 * @param bound  the upper bound (exclusive). Must be positive.
	 * @param unit   the {@link TemporalUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Duration}
	 *         of the original {@link DelayTimeProviderChain} with the added amount
	 */
	public static DelayTimeProviderChain plusRandomDelay(DelayTimeProviderChain origin, int bound, TemporalUnit unit) {
		return new DelayTimeProviderChain(() -> {
			return origin.getDelay().plus(Delay.of(Duration.of(new Random().nextInt(bound), unit)));
		});
	}

	/**
	 * Helper method to create a new {@link DelayTimeProviderChain} with a random
	 * amount added between 0 (inclusive) and the bound (exclusive).
	 * 
	 * @implNote delegates to
	 *           {@link #plusRandomDelay(DelayTimeProviderChain, int, TimeUnit)}
	 * @param bound the upper bound (exclusive). Must be positive.
	 * @param unit  the {@link TemporalUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Duration}
	 *         of the original {@link DelayTimeProviderChain} with the added amount
	 * @see #plusRandomDelay(DelayTimeProviderChain, int, TimeUnit)
	 */
	public DelayTimeProviderChain plusRandomDelay(int bound, TemporalUnit unit) {
		return plusRandomDelay(this, bound, unit);
	}

}