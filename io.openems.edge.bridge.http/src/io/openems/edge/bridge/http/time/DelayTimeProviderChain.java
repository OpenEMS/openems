package io.openems.edge.bridge.http.time;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;

import io.openems.common.timedata.DurationUnit;

public class DelayTimeProviderChain {

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a the
	 * {@link Delay#zero()} on request.
	 * 
	 * @return a {@link DelayTimeProviderChain} of zero delay
	 */
	public static DelayTimeProviderChain immediate() {
		return new DelayTimeProviderChain(() -> Delay.zero());
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a fixed {@link Delay}
	 * on request.
	 * 
	 * @param delay the {@link Delay} to return when requested
	 * @return a {@link DelayTimeProviderChain} of the given {@link Delay}
	 */
	public static DelayTimeProviderChain fixedDelay(Delay delay) {
		return new DelayTimeProviderChain(() -> delay);
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which returns a {@link Delay} till
	 * the next truncated time of the given {@link DurationUnit}.
	 * 
	 * <p>
	 * e. g. if a <code>DurationUnit.ofMinutes(1)</code> gets provided the
	 * {@link Delay} at the time 12h 43min 23sec would be (60sec - 23sec) = 37sec to
	 * 12h 44min 0sec. Same would work for every hour 12h, 13h, 14h, ... with
	 * <code>DurationUnit.ofHours(1)</code>
	 * 
	 * @param clock        the {@link Clock} to get the current time
	 * @param durationUnit the {@link DurationUnit} to truncate with
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay}
	 */
	public static DelayTimeProviderChain fixedAtEveryFull(Clock clock, DurationUnit durationUnit) {
		return new DelayTimeProviderChain(() -> {
			final var now = LocalDateTime.now(clock);
			final var waitSeconds = now.until(now.truncatedTo(durationUnit) //
					.plus(durationUnit.getDuration()), ChronoUnit.MILLIS);

			return new Delay(waitSeconds, TimeUnit.MILLISECONDS);
		});
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
	 * the given amount. The new provided {@link Delay} gets rounded to seconds
	 * based on the implementation of {@link Delay#plus(Delay)}.
	 * 
	 * @param origin the original {@link DelayTimeProviderChain} to get the initial
	 *               {@link Delay}
	 * @param amount the amount to add to the initial {@link Delay}
	 * @param unit   the {@link TimeUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay} of
	 *         the original {@link DelayTimeProviderChain} with the added amount
	 */
	public static DelayTimeProviderChain plusFixedAmount(DelayTimeProviderChain origin, int amount, TimeUnit unit) {
		return new DelayTimeProviderChain(() -> {
			return origin.getDelay().plus(new Delay(amount, unit));
		});
	}

	/**
	 * Helper method to create a new {@link DelayTimeProviderChain} with a fixed
	 * amount added.
	 * 
	 * @implNote delegates to
	 *           {@link #plusFixedAmount(DelayTimeProviderChain, int, TimeUnit)}
	 * @param amount the amount to add to the initial {@link Delay}
	 * @param unit   the {@link TimeUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay} of
	 *         the original {@link DelayTimeProviderChain} with the added amount
	 * @see #plusFixedAmount(DelayTimeProviderChain, int, TimeUnit)
	 */
	public DelayTimeProviderChain plusFixedAmount(int amount, TimeUnit unit) {
		return plusFixedAmount(this, amount, unit);
	}

	/**
	 * Creates a {@link DelayTimeProviderChain} which adds to the original provider
	 * a random amount between 0 (inclusive) and bound (exclusive). The new provided
	 * {@link Delay} gets rounded to seconds based on the implementation of
	 * {@link Delay#plus(Delay)}.
	 * 
	 * @param origin the original {@link DelayTimeProviderChain} to get the initial
	 *               {@link Delay}
	 * @param bound  the upper bound (exclusive). Must be positive.
	 * @param unit   the {@link TimeUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay} of
	 *         the original {@link DelayTimeProviderChain} with the added amount
	 */
	public static DelayTimeProviderChain plusRandomDelay(DelayTimeProviderChain origin, int bound, TimeUnit unit) {
		return new DelayTimeProviderChain(() -> {
			return origin.getDelay().plus(new Delay(new Random().nextInt(bound), unit));
		});
	}

	/**
	 * Helper method to create a new {@link DelayTimeProviderChain} with a random
	 * amount added between 0 (inclusive) and the bound (exclusive).
	 * 
	 * @implNote delegates to
	 *           {@link #plusRandomDelay(DelayTimeProviderChain, int, TimeUnit)}
	 * @param bound the upper bound (exclusive). Must be positive.
	 * @param unit  the {@link TimeUnit} of the amount to add
	 * @return a {@link DelayTimeProviderChain} which returns the {@link Delay} of
	 *         the original {@link DelayTimeProviderChain} with the added amount
	 * @see #plusRandomDelay(DelayTimeProviderChain, int, TimeUnit)
	 */
	public DelayTimeProviderChain plusRandomDelay(int bound, TimeUnit unit) {
		return plusRandomDelay(this, bound, unit);
	}

}