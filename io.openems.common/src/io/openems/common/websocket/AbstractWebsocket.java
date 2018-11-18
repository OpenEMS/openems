package io.openems.common.websocket;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractWebsocket {

	private final String name;

	protected abstract OnInternalError getOnInternalError();

	protected abstract OnOpen getOnOpen();

	protected abstract OnRequest getOnRequest();

	protected abstract OnNotification getOnNotification();

	protected abstract OnError getOnError();

	protected abstract OnClose getOnClose();

	public AbstractWebsocket(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Handles an internal Error asynchronously
	 * 
	 * @param e
	 */
	protected void handleInternalErrorAsync(Exception e) {
		CompletableFuture.runAsync(new OnInternalErrorHandler(this.getOnInternalError(), e));
	}

	/**
	 * Handles an internal Error synchronously
	 * 
	 * @param e
	 */
	protected void handleInternalErrorSync(Exception e) {
		this.getOnInternalError().run(e);
	}

}
