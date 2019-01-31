package io.openems.common.websocket;

public class OnInternalErrorHandler implements Runnable {

	private final OnInternalError onInternalError;
	private final Exception ex;

	public OnInternalErrorHandler(OnInternalError onInternalError, Exception ex) {
		this.onInternalError = onInternalError;
		this.ex = ex;
	}

	@Override
	public final void run() {
		this.onInternalError.run(this.ex, "");
	}

}
