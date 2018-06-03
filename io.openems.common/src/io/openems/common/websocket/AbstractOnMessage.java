package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class AbstractOnMessage implements Runnable {

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
			JsonObject jMessage = (new JsonParser()).parse(this.message).getAsJsonObject();
			this.run(this.websocket, jMessage);
		} catch (Throwable e) {
			log.error("onMessage-Error [" + this.message + "]: ");
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, JsonObject jMessage);

}
