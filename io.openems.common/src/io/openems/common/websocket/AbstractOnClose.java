package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOnClose implements Runnable {

	private final Logger log = LoggerFactory.getLogger(AbstractOnClose.class);

	protected final WebSocket websocket;
	private final int code;
	private final String reason;
	private final boolean remote;

	public AbstractOnClose(WebSocket websocket, int code, String reason, boolean remote) {
		this.websocket = websocket;
		this.code = code;
		this.reason = reason;
		this.remote = remote;
	}

	@Override
	public final void run() {
		try {
			this.run(this.websocket, this.code, this.reason, this.remote);
		} catch (Throwable e) {
			log.error("onClose-Error. Code [" + this.code + "] Reason [" + this.reason + "]: " + e.getMessage());
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, int code, String reason, boolean remote);

}
