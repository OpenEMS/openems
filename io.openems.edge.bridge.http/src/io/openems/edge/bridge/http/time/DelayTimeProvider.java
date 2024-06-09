package io.openems.edge.bridge.http.time;

import java.time.Duration;
import java.util.Objects;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;

public interface DelayTimeProvider {

	public static sealed interface Delay {

		/**
		 * Creates a {@link Delay} which represents delay of zero.
		 * 
		 * @return a {@link Delay} which represents delay of zero.
		 */
		public static Delay immediate() {
			return Delay.of(Duration.ZERO);
		}

		/**
		 * Creates a {@link Delay} which represents a delay of the given
		 * {@link Duration}.
		 * 
		 * @param duration the {@link Duration} of the {@link Delay}
		 * @return a {@link Delay} of the {@link Duration}.
		 */
		public static Delay of(Duration duration) {
			return new DurationDelay(duration);
		}

		/**
		 * Creates a {@link Delay} which represents a infinite delay.
		 * 
		 * @return a {@link Delay} which never ends.
		 */
		public static Delay infinite() {
			return InfiniteDelay.INSTANCE;
		}

		/**
		 * Adds the provided {@link Delay} to the current {@link Delay} and returns its
		 * result.
		 * 
		 * @param delay the {@link Delay} to add
		 * @return the result
		 */
		public Delay plus(Delay delay);

		public static final class DurationDelay implements Delay {
			private final Duration duration;

			private DurationDelay(Duration duration) {
				super();
				this.duration = duration;
			}

			public Duration getDuration() {
				return this.duration;
			}

			@Override
			public Delay plus(Delay delay) {
				if (delay instanceof DurationDelay durationDelay) {
					return new DurationDelay(this.duration.plus(durationDelay.getDuration()));
				}
				return delay.plus(this);
			}

			@Override
			public int hashCode() {
				return Objects.hash(this.duration);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj instanceof DurationDelay other) {
					return Objects.equals(this.duration, other.duration);
				}
				return false;
			}

		}

		public static final class InfiniteDelay implements Delay {

			public static final InfiniteDelay INSTANCE = new InfiniteDelay();

			private InfiniteDelay() {
			}

			@Override
			public Delay plus(Delay delay) {
				return this;
			}

		}

	}

	/**
	 * Gives the {@link Delay} till the next run should be triggered on the first
	 * trigger.
	 * 
	 * @return the {@link Delay} till the next run
	 */
	public Delay onFirstRunDelay();

	/**
	 * Gives the {@link Delay} till the next run should be triggered when the last
	 * run completed with an error.
	 * 
	 * @param error the {@link HttpError} which happened
	 * @return the {@link Delay} till the next run
	 */
	public Delay onErrorRunDelay(HttpError error);

	/**
	 * Gives the {@link Delay} till the next run should be triggered when the last
	 * run completed successfully.
	 * 
	 * @param result the result of the last run
	 * @return the {@link Delay} till the next run
	 */
	public Delay onSuccessRunDelay(HttpResponse<String> result);

}
