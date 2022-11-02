package io.openems.common.websocket;

public class OnInternalErrorHandler implements Runnable {

	private final OnInternalError onInternalError;
	private final Throwable t;
	private final String wsDataString;

	public OnInternalErrorHandler(OnInternalError onInternalError, Throwable t, String wsDataString) {
		this.onInternalError = onInternalError;
		this.t = t;
		this.wsDataString = wsDataString;
	}

	@Override
	public final void run() {
		this.onInternalError.run(this.t, this.wsDataString);
	}

}
