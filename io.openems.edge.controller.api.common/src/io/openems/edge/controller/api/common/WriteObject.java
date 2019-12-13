package io.openems.edge.controller.api.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.WriteChannel;

public abstract class WriteObject {

	private final List<Runnable> onSuccessCallbacks = new CopyOnWriteArrayList<>();
	private final List<Runnable> onFirstSuccessCallbacks = new CopyOnWriteArrayList<>();
	private boolean notifiedSuccess = false;
	private final List<Consumer<OpenemsException>> onErrorCallbacks = new CopyOnWriteArrayList<>();
	private final List<Consumer<OpenemsException>> onFirstErrorCallbacks = new CopyOnWriteArrayList<>();
	private boolean notifiedError = false;
	private final List<Runnable> onTimeoutCallbacks = new CopyOnWriteArrayList<>();

	public WriteObject onSuccess(Runnable callback) {
		this.onSuccessCallbacks.add(callback);
		return this;
	}

	public WriteObject onFirstSuccess(Runnable callback) {
		this.onFirstSuccessCallbacks.add(callback);
		return this;
	}

	public WriteObject onError(Consumer<OpenemsException> callback) {
		this.onErrorCallbacks.add(callback);
		return this;
	}

	public WriteObject onFirstError(Consumer<OpenemsException> callback) {
		this.onFirstErrorCallbacks.add(callback);
		return this;
	}

	public WriteObject onTimeout(Runnable callback) {
		this.onTimeoutCallbacks.add(callback);
		return this;
	}

	public void notifySuccess() {
		if (!this.notifiedSuccess) {
			this.onFirstSuccessCallbacks.forEach(callback -> callback.run());
			this.notifiedSuccess = true;
		}
		this.onSuccessCallbacks.forEach(callback -> callback.run());
	}

	public void notifyError(OpenemsException e) {
		if (!this.notifiedError) {
			this.onFirstErrorCallbacks.forEach(callback -> callback.accept(e));
			this.notifiedError = true;
		}
		this.onErrorCallbacks.forEach(callback -> callback.accept(e));
	}

	public void notifyTimeout() {
		this.onTimeoutCallbacks.forEach(callback -> callback.run());
	}

	public abstract void setNextWriteValue(WriteChannel<?> writeChannel) throws OpenemsNamedException;

	public abstract String valueToString();
	
	public abstract boolean isNull();
}
