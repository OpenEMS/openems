package io.openems.edge.common.update;

import static io.openems.edge.common.update.Progress.assertPercentage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.Tuple2;

public class ProgressPublisher {

	private final Logger log = LoggerFactory.getLogger(ProgressPublisher.class);

	private final List<Consumer<Progress>> listener = new ArrayList<>();

	/**
	 * Creates a subprogress which can be used from 0 to 100 and updates this
	 * progress accordingly.
	 * 
	 * @param start the start of the current progress
	 * @param end   the end of the current progress
	 * @return the {@link ProgressPublisher Subprogress}
	 */
	public ProgressPublisher subProgress(int start, int end) {
		final var subProgress = new ProgressPublisher();

		subProgress.addListener(progress -> {
			this.setPercentage(start + (int) ((end - start) / 100.0 * progress.percentage()), progress.title());
		});

		return subProgress;
	}

	/**
	 * Creates a for i loop.
	 * 
	 * @param start the start of the loop
	 * @param end   the end of the loop
	 * @param step  the step size of one element
	 * @param title the title to set
	 * @return a {@link Iterable}
	 */
	public Iterable<Integer> fori(int start, int end, int step, String title) {
		return () -> new Iterator<>() {

			private int counter = start;

			@Override
			public boolean hasNext() {
				return this.counter < end;
			}

			@Override
			public Integer next() {
				final var prev = this.counter;
				this.counter += step;
				ProgressPublisher.this.setPercentage((int) ((((double) this.counter) - start) / (end - start) * 100.0),
						title + " [" + (this.counter / step) + "|" + ((end - start) / step) + "]");
				return prev;
			}
		};
	}

	public void setPercentageByStep(int step, int totalSteps) {
		this.setPercentageByStep(step, totalSteps, null);
	}

	public void setPercentageByStep(int step, int totalSteps, String stepTitle) {
		int percentage = (int) Math.floor(((double) step / totalSteps) * 100.0);
		this.setPercentage(percentage, stepTitle);
	}

	public void setPercentage(int percentage) {
		this.setPercentage(percentage, null);
	}

	public void setPercentage(int percentage, String stepTitle) {
		this.notifyListener(new Progress(percentage, stepTitle));
	}

	/**
	 * Sleeps and updates the current progress if changed.
	 * 
	 * @param millis the time to wait
	 * @param start  the start percentage
	 * @param end    the end percentage
	 * @param title  the title for the progress
	 * @throws InterruptedException on {@link Thread#interrupt()}
	 * @see Thread#sleep(long)
	 */
	public void sleep(long millis, int start, int end, String title) throws InterruptedException {
		if (end < start) {
			throw new IllegalArgumentException("start must be smaller than end");
		}
		assertPercentage(start);
		assertPercentage(end);

		var numberOfSleepSteps = end - start;
		var sleepStep = millis / numberOfSleepSteps;

		this.setPercentage(start, title);
		for (int i = 0; i < numberOfSleepSteps; i++) {
			this.setPercentage(start + i);
			Thread.sleep(sleepStep);
		}
		Thread.sleep(millis - (sleepStep * numberOfSleepSteps));
		this.setPercentage(end);
	}

	private void notifyListener(Progress progress) {
		for (var listener : this.listener) {
			try {
				listener.accept(progress);
			} catch (Exception e) {
				this.log.warn("Unexpected error while executing listener.", e);
			}
		}
	}

	/**
	 * Adds a listener to the progress.
	 * 
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Consumer<Progress> addListener(Consumer<Progress> listener) {
		this.listener.add(listener);
		return listener;
	}

	/**
	 * Removes a listener.
	 * 
	 * @param listener the listener to remove
	 * @return true if the listener was registered
	 */
	public boolean removeListener(Consumer<Progress> listener) {
		return this.listener.remove(listener);
	}

	/**
	 * Creates an {@link Iterable} that provides a dedicated
	 * {@link ProgressPublisher} for each element.
	 *
	 * <p>
	 * The overall progress is evenly split across all elements. The progress title
	 * is updated when the next element is requested from the iterator.
	 *
	 * @param <T>    the type of the elements
	 * @param start  the start percentage of the overall progress
	 * @param end    the end percentage of the overall progress
	 * @param values the elements to iterate over
	 * @param title  the title prefix
	 * @return an {@link Iterable} providing a sub progress and its element
	 */
	public <T> Iterable<Tuple2<ProgressPublisher, T>> forEachWithProgress(int start, int end, Iterable<T> values,
			String title) {

		var list = new ArrayList<T>();
		values.forEach(list::add);

		return () -> new Iterator<>() {

			private final Iterator<T> iterator = list.iterator();
			private int index = 0;

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public Tuple2<ProgressPublisher, T> next() {
				final var nextElement = this.iterator.next();
				final var currentIndex = this.index++;

				int subStart = start + currentIndex * (end - start) / list.size();
				int subEnd = start + (currentIndex + 1) * (end - start) / list.size();

				// Show which element is currently processed
				ProgressPublisher.this.setPercentage(//
						subStart, //
						title + " [" + (currentIndex + 1) + "/" + list.size() + "]");

				return new Tuple2<>(//
						ProgressPublisher.this.subProgress(subStart, subEnd), //
						nextElement);
			}
		};
	}

}
