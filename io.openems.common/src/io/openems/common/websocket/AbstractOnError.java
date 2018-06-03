package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOnError implements Runnable {

	private final Logger log = LoggerFactory.getLogger(AbstractOnError.class);

	protected final WebSocket websocket;
	protected final Exception ex;

	public AbstractOnError(WebSocket websocket, Exception ex) {
		this.websocket = websocket;
		this.ex = ex;
	}

	@Override
	public final void run() {
		try {
			this.run(this.websocket, this.ex);
		} catch (Throwable e) {
			log.error("onError handling of Exception [" + ex.getMessage() + "] failed: " + e.getMessage());
			ex.printStackTrace();
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, Exception ex);

}
