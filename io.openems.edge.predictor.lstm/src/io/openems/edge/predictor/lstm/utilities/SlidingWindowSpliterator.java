package io.openems.edge.predictor.lstm.utilities;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SlidingWindowSpliterator<T> implements Spliterator<Stream<T>> {

	/**
	 * creates windows.
	 * 
	 * @param <T>        generic data type
	 * @param stream     Collection
	 * @param windowSize size of the window
	 * @return result List of List
	 */
	public static <T> Stream<Stream<T>> windowed(Collection<T> stream, int windowSize) {
		return StreamSupport.stream(new SlidingWindowSpliterator<>(stream, windowSize), false);
	}

	private final Queue<T> buffer;
	private final Iterator<T> sourceIterator;
	private final int windowSize;
	private final int size;

	private SlidingWindowSpliterator(Collection<T> source, int windowSize) {
		this.buffer = new ArrayDeque<>(windowSize);
		this.sourceIterator = Objects.requireNonNull(source).iterator();
		this.windowSize = windowSize;
		this.size = calculateSize(source, windowSize);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean tryAdvance(Consumer<? super Stream<T>> action) {
		if (this.windowSize < 1) {
			return false;
		}

		while (this.sourceIterator.hasNext()) {
			this.buffer.add(this.sourceIterator.next());

			if (this.buffer.size() == this.windowSize) {
				action.accept(Arrays.stream((T[]) this.buffer.toArray(new Object[0])));
				this.buffer.poll();
				return true;
			}
		}

		return false;
	}

	@Override
	public Spliterator<Stream<T>> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return this.size;
	}

	@Override
	public int characteristics() {
		return ORDERED | NONNULL | SIZED;
	}

	private static int calculateSize(Collection<?> source, int windowSize) {
		return source.size() < windowSize ? 0 : source.size() - windowSize + 1;
	}
}
