package io.openems.common.utils;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A variant of Subject that requires an initial value and emits its current
 * value whenever it is subscribed to.
 */
public class BehaviorSubject<T> {
	private final Logger log = LoggerFactory.getLogger(BehaviorSubject.class);

	private volatile T value;
	private final List<Consumer<T>> consumers = new CopyOnWriteArrayList<>();

	public BehaviorSubject(T initialValue) {
		this.value = initialValue;
	}

	/**
	 * Subscribes to this subject to get notified about value changes.
	 * 
	 * @param func Gets called when the value changes.
	 */
	public void subscribe(Consumer<T> func) {
		this.consumers.add(func);
		func.accept(this.value);
	}

	/**
	 * Unsubscribes a subscription to a value change. Don't forget to call this
	 * method when your instance is destroyed!
	 * 
	 * @param func Function that was used in subscribe() call.
	 */
	public void unsubscribe(Consumer<T> func) {
		this.consumers.remove(func);
	}

	/**
	 * Checks whether any consumer is currently subscribed.
	 *
	 * @return true if there is at least one subscriber
	 */
	public boolean hasSubscribers() {
		return !this.consumers.isEmpty();
	}

	/**
	 * Returns the last value.
	 *
	 * @return Value
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * Sets new value into the subject. This will change the value returned from
	 * getValue() and it publishes the new value to all subscribers synchronously.
	 * 
	 * @param value New value
	 */
	public void setValue(T value) {
		this.updateValue(v -> value);
	}

	/**
	 * Returns an {@link CompletableFuture} that returns when there is a non-value
	 * present in this BehaviorSubject. The future throws an error if maxWait time
	 * is reached.
	 * 
	 * @param maxWait Max duration to wait before a {@link TimeoutException} is
	 *                thrown in the future.
	 * @return {@link CompletableFuture} with either the non-null value or a
	 *         {@link TimeoutException}
	 */
	public CompletableFuture<T> waitForNonNullValue(Duration maxWait) {
		return this.waitUntil(Objects::nonNull, maxWait);
	}

	/**
	 * Returns an {@link CompletableFuture} that returns when the check function
	 * returns true for the value present in this BehaviorSubject. The future throws
	 * an error if maxWait time is reached.
	 *
	 * @param check   This check needs to be true for the future to complete. The
	 *                check is called with the current value of the BehaviorSubject.
	 * @param maxWait Max duration to wait before a {@link TimeoutException} is
	 *                thrown in the future.
	 * @return {@link CompletableFuture} with either the non-null value or a
	 *         {@link TimeoutException}
	 */
	public CompletableFuture<T> waitUntil(Predicate<T> check, Duration maxWait) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Consumer<T> subscribeFunction = (T val) -> {
			if (check.test(val)) {
				future.complete(val);
			}
		};

		this.subscribe(subscribeFunction);

		return future.orTimeout(maxWait.toMillis(), TimeUnit.MILLISECONDS).whenComplete((val, ex) -> {
			this.unsubscribe(subscribeFunction);
		});
	}

	/**
	 * Updates the current value in the subject by applying the given function to
	 * the current value. This will change the value returned from getValue() and it
	 * publishes the new value to all subscribers synchronously.
	 * 
	 * @param updateFunction the update function
	 */
	public synchronized void updateValue(UnaryOperator<T> updateFunction) {
		this.value = updateFunction.apply(this.value);
		this.consumers.forEach(consumer -> {
			try {
				consumer.accept(this.value);
			} catch (Exception ex) {
				this.log.error("BehaviorSubject Subscriber caused an exception", ex);
			}
		});
	}

	/**
	 * Returns the amount of subscribers to this BehaviorSubject.
	 * 
	 * @return amount
	 */
	public int getAmountOfSubscribers() {
		return this.consumers.size();
	}

}
