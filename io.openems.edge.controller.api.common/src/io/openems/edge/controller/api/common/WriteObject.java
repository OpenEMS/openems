package io.openems.edge.controller.api.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.WriteChannel;

public abstract class WriteObject {

	private final List<Runnable> onSuccessCallbacks = new CopyOnWriteArrayList<>();
	private final List<Runnable> onFirstSuccessCallbacks = new CopyOnWriteArrayList<>();
	private boolean notifiedSuccess = false;
	private final List<Consumer<OpenemsException>> onErrorCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<OpenemsException>> onFirstErrorCallbacks = new CopyOnWriteArrayList<>();
	private boolean notifiedError = false;
	private final List<Runnable> onTimeoutCallbacks = new CopyOnWriteArrayList<>();

	/**
	 * Callback on successful setting of the value.
	 *
	 * @param callback a callback {@link Runnable}
	 * @return myself
	 */
	public WriteObject onSuccess(Runnable callback) {
		this.onSuccessCallbacks.add(callback);
		return this;
	}

	/**
	 * Callback on first successful setting of the value.
	 *
	 * @param callback a callback {@link Runnable}
	 * @return myself
	 */
	public WriteObject onFirstSuccess(Runnable callback) {
		this.onFirstSuccessCallbacks.add(callback);
		return this;
	}

	/**
	 * Callback on error while setting the value.
	 *
	 * @param callback a callback {@link Runnable}
	 * @return myself
	 */
	public WriteObject onError(Consumer<OpenemsException> callback) {
		this.onErrorCallbacks.add(callback);
		return this;
	}

	/**
	 * Callback on first error while setting the value.
	 *
	 * @param callback a callback {@link Runnable}
	 * @return myself
	 */
	public WriteObject onFirstError(Consumer<OpenemsException> callback) {
		this.onFirstErrorCallbacks.add(callback);
		return this;
	}

	/**
	 * Callback on timeout while setting the value.
	 *
	 * @param callback a callback {@link Runnable}
	 * @return myself
	 */
	public WriteObject onTimeout(Runnable callback) {
		this.onTimeoutCallbacks.add(callback);
		return this;
	}

	/**
	 * Notify success for setting the value.
	 */
	public void notifySuccess() {
		if (!this.notifiedSuccess) {
			this.onFirstSuccessCallbacks.forEach(Runnable::run);
			this.notifiedSuccess = true;
		}
		this.onSuccessCallbacks.forEach(Runnable::run);
	}

	/**
	 * Notify error while setting the value.
	 *
	 * @param e the {@link OpenemsException}
	 */
	public void notifyError(OpenemsException e) {
		if (!this.notifiedError) {
			this.onFirstErrorCallbacks.forEach(callback -> callback.accept(e));
			this.notifiedError = true;
		}
		this.onErrorCallbacks.forEach(callback -> callback.accept(e));
	}

	/**
	 * Notify a timeout for setting the value.
	 */
	public void notifyTimeout() {
		this.onTimeoutCallbacks.forEach(Runnable::run);
	}

	/**
	 * Set the next write value of the Channel.
	 *
	 * @param writeChannel the {@link WriteChannel}
	 * @throws OpenemsNamedException on error
	 */
	public abstract void setNextWriteValue(WriteChannel<?> writeChannel) throws OpenemsNamedException;

	/**
	 * Gets the value as a String for logging purposes.
	 *
	 * @return the value as String
	 */
	public abstract String valueToString();

	/**
	 * Is there a defined value?.
	 *
	 * @return true if no value is there; false if a value is available.
	 */
	public abstract boolean isNull();
}
