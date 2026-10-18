package io.openems.common.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.openems.common.function.Disposable;

public class EventEmitterTester<T> implements Disposable {
	private final EventEmitter<T> emitter;
	private final List<T> emittedValues = new CopyOnWriteArrayList<>();

	private final Consumer<T> listener = this::onEvent;

	/**
	 * Creates a tester that subscribes to the given {@link EventEmitter} and
	 * records all emitted values.
	 *
	 * @param emitter the emitter to observe
	 */
	public EventEmitterTester(EventEmitter<T> emitter) {
		this.emitter = emitter;
		this.emitter.subscribe(this.listener);
	}

	private void onEvent(T value) {
		this.emittedValues.add(value);
	}

	/**
	 * Checks whether the given value was emitted at least once.
	 *
	 * @param value the value to look for
	 * @return {@code true} if the value was emitted; otherwise {@code false}
	 */
	public boolean checkIfValueWasEmitted(T value) {
		for (var emittedValue : this.emittedValues) {
			if (Objects.equals(emittedValue, value)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Asserts that the given value was emitted.
	 *
	 * @param value the expected emitted value
	 * @param reset if {@code true}, clears recorded values after assertion
	 * @throws RuntimeException if the expected value was not emitted
	 */
	public void assertEmitted(T value, boolean reset) {
		if (!this.checkIfValueWasEmitted(value)) {
			throw new RuntimeException("Expected value '" + value + "' was not emitted by the EventEmitter.");
		}

		if (reset) {
			this.reset();
		}
	}

	/**
	 * Clears all currently recorded emitted values.
	 */
	public void reset() {
		this.emittedValues.clear();
	}

	/**
	 * Unsubscribes this tester from the observed emitter.
	 */
	@Override
	public void dispose() {
		this.emitter.unsubscribe(this.listener);
	}
}
