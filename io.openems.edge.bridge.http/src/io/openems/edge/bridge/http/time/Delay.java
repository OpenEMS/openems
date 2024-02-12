package io.openems.edge.bridge.http.time;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public record Delay(//
		long amount, //
		TimeUnit unit //
) {

	/**
	 * Returns a Delay with amount 0.
	 * 
	 * @return the {@link Delay}
	 */
	public static Delay zero() {
		return new Delay(0, TimeUnit.SECONDS);
	}

	/**
	 * Creates a new {@link Delay} from the sum of this {@link Delay} and the
	 * provided {@link Delay}.
	 * 
	 * <p>
	 * Note: The sum gets rounded to seconds
	 * 
	 * @param delay the {@link Delay} to add
	 * @return the new Delay with the sum of the two Delays
	 */
	public Delay plus(Delay delay) {
		final var thisDur = Duration.of(this.amount(), this.unit().toChronoUnit());
		final var otherDur = Duration.of(delay.amount(), delay.unit().toChronoUnit());

		return new Delay(thisDur.plus(otherDur).get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
	}

}