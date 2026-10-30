package io.openems.common.event;

import java.util.function.Consumer;

/**
 * Contains methods to subscribe and unsubscribe to events of type {@code T}.
 */
public interface Subscribable<T> {
	/**
	 * Subscribes a listener that is called for each emitted event.
	 *
	 * @param func the listener callback
	 */
	void subscribe(Consumer<? super T> func);

	/**
	 * Unsubscribes a previously subscribed listener.
	 *
	 * @param func the listener callback to remove, make sure that it's the same
	 *             instance (this::xxx is not sharing the same instance!)
	 * @return {@code true} if the listener was removed, otherwise {@code false}
	 */
	boolean unsubscribe(Consumer<? super T> func);
}
