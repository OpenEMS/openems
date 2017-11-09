package io.openems.core.utilities.api;

import java.util.Optional;

import io.openems.api.channel.WriteChannel;
import io.openems.common.exceptions.OpenemsException;

public abstract class WriteObject {
	public interface OnSuccess {
		public void run();
	}

	public interface OnError {
		public void run(OpenemsException e);
	}

	public interface OnTimeout {
		public void run();
	}

	private Optional<OnSuccess> onSuccessOpt = Optional.empty();
	private boolean notifiedSuccess = false;
	private Optional<OnSuccess> onFirstSuccessOpt = Optional.empty();
	private Optional<OnError> onErrorOpt = Optional.empty();
	private boolean notifiedError = false;
	private Optional<OnError> onFirstErrorOpt = Optional.empty();
	private Optional<OnTimeout> onTimeoutOpt = Optional.empty();

	public WriteObject onSuccess(OnSuccess handler) {
		this.onSuccessOpt = Optional.ofNullable(handler);
		return this;
	}

	public WriteObject onFirstSuccess(OnSuccess handler) {
		this.onFirstSuccessOpt = Optional.ofNullable(handler);
		return this;
	}

	public WriteObject onError(OnError handler) {
		this.onErrorOpt = Optional.ofNullable(handler);
		return this;
	}

	public WriteObject onFirstError(OnError handler) {
		this.onFirstErrorOpt = Optional.ofNullable(handler);
		return this;
	}

	public WriteObject onTimeout(OnTimeout handler) {
		this.onTimeoutOpt = Optional.ofNullable(handler);
		return this;
	}

	public void notifySuccess() {
		if (!this.notifiedSuccess) {
			if (this.onFirstSuccessOpt.isPresent()) {
				this.onFirstSuccessOpt.get().run();
			}
			this.notifiedSuccess = true;
		}
		if (this.onSuccessOpt.isPresent()) {
			this.onSuccessOpt.get().run();
		}
	}

	public void notifyError(OpenemsException e) {
		if (!this.notifiedError) {
			if (this.onFirstErrorOpt.isPresent()) {
				this.onFirstErrorOpt.get().run(e);
			}
			this.notifiedError = true;
		}
		if (this.onErrorOpt.isPresent()) {
			this.onErrorOpt.get().run(e);
		}
	}

	public void notifyTimeout() {
		if (this.onTimeoutOpt.isPresent()) {
			this.onTimeoutOpt.get().run();
		}
	}

	public abstract void pushWrite(WriteChannel<?> writeChannel) throws OpenemsException;

	public abstract String valueToString();
}
