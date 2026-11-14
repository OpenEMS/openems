package io.openems.common.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;

import io.openems.common.logger.ContextLogger;

public class EventEmitter<T> implements Subscribable<T> {
	private final Logger log;
	private final List<Consumer<? super T>> subscribers = new CopyOnWriteArrayList<>();

	public EventEmitter() {
		this.log = new ContextLogger(EventEmitter.class, "EventEmitter "
				+ StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getName());
	}

	@Override
	public void subscribe(Consumer<? super T> func) {
		this.subscribers.add(func);
	}

	@Override
	public boolean unsubscribe(Consumer<? super T> func) {
		return this.subscribers.remove(func);
	}

	/**
	 * Emit event to all subscribed listeners. All calls are made synchronously, but
	 * exceptions are handled.
	 *
	 * @param value Event to emit.
	 */
	public void emit(T value) {
		for (Consumer<? super T> subscriber : this.subscribers) {
			try {
				subscriber.accept(value);
			} catch (Exception e) {
				this.log.error("Event subscription has thrown an exception (Subscriber: "
						+ subscriber.getClass().getName() + ", Value: '" + value + "')", e);
			}
		}
	}

}
