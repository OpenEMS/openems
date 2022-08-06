package io.openems.common.websocket;

public class OnInternalErrorHandler implements Runnable {

	private final OnInternalError onInternalError;
	private final Exception ex;
	private final String wsDataString;

	public OnInternalErrorHandler(OnInternalError onInternalError, Exception ex, String wsDataString) {
		this.onInternalError = onInternalError;
		this.ex = ex;
		this.wsDataString = wsDataString;
	}

	@Override
	public final void run() {
		this.onInternalError.run(this.ex, this.wsDataString);
	}

}
