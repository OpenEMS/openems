package io.openems.core.utilities.api;

import java.util.Optional;

import io.openems.api.exception.WriteChannelException;

public class WriteObject {
	public interface OnSuccess {
		public void run();
	}

	public interface OnError {
		public void run(WriteChannelException e);
	}

	public final Object value;

	private final Optional<OnSuccess> onSuccessOpt;
	private final Optional<OnError> onErrorOpt;

	public WriteObject(Object value) {
		this.value = value;
		this.onSuccessOpt = Optional.empty();
		this.onErrorOpt = Optional.empty();
	}

	public WriteObject(Object value, OnSuccess onSuccess, OnError onError) {
		this.value = value;
		this.onSuccessOpt = Optional.ofNullable(onSuccess);
		this.onErrorOpt = Optional.ofNullable(onError);
	}

	public void onSuccess() {
		if(this.onSuccessOpt.isPresent()) {
			this.onSuccessOpt.get().run();
		}
	}

	public void onError(WriteChannelException e) {
		if(this.onErrorOpt.isPresent()) {
			this.onErrorOpt.get().run(e);
		}
	}
}
