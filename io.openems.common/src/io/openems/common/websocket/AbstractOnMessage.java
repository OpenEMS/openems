package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOnMessage implements Runnable {

	public static final String COMPATIBILITY_METHOD = "_compatibility";

	private final Logger log = LoggerFactory.getLogger(AbstractOnMessage.class);

	protected final WebSocket websocket;
	protected final String message;

	public AbstractOnMessage(WebSocket websocket, String message) {
		this.websocket = websocket;
		this.message = message;
	}

	@Override
	public final void run() {
		try {
			this.run(this.websocket, message);
		} catch (Throwable e) {
			log.error("onMessage-Error [" + this.message + "]: ");
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, String message);

}
