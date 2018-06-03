package io.openems.common.websocket;

import java.util.Iterator;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public abstract class AbstractOnOpen implements Runnable {

	private final Logger log = LoggerFactory.getLogger(AbstractOnOpen.class);

	protected final WebSocket websocket;
	protected final JsonObject jHandshake;

	public AbstractOnOpen(WebSocket websocket, ClientHandshake handshake) {
		this.websocket = websocket;
		this.jHandshake = this.handshakeToJsonObject(handshake);
	}

	@Override
	public final void run() {
		try {
			this.run(this.websocket, this.jHandshake);
		} catch (Throwable e) {
			log.error("onOpen-Error [" + this.jHandshake + "]: ");
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, JsonObject jHandshake);

	/**
	 * Converts a Handshake to a JsonObject
	 * 
	 * @param handshake
	 * @return
	 */
	protected JsonObject handshakeToJsonObject(ClientHandshake handshake) {
		JsonObject j = new JsonObject();
		for (Iterator<String> iter = handshake.iterateHttpFields(); iter.hasNext();) {
			String field = iter.next();
			j.addProperty(field, handshake.getFieldValue(field));
		}
		return j;
	}
}
