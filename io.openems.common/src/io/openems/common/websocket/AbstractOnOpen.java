package io.openems.common.websocket;

import java.util.Iterator;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public abstract class AbstractOnOpen implements Runnable {

	private final Logger log = LoggerFactory.getLogger(AbstractOnOpen.class);

	protected final WebSocket websocket;
	protected final ClientHandshake handshake;

	public AbstractOnOpen(WebSocket websocket, ClientHandshake handshake) {
		this.websocket = websocket;
		this.handshake = handshake;
	}

	@Override
	public final void run() {
		try {
			this.run(this.websocket, handshake);
		} catch (Throwable e) {
			log.error("onOpen-Error [" + this.handshakeToJsonObject(handshake).toString() + "]: ");
			e.printStackTrace();
		}
	}

	protected abstract void run(WebSocket websocket, ClientHandshake handshake);

	/**
	 * Converts a Handshake to a JsonObject.
	 * 
	 * @param handshake the ClientHandshake
	 * @return the Handshake as a JsonObject
	 */
	private JsonObject handshakeToJsonObject(ClientHandshake handshake) {
		JsonObject j = new JsonObject();
		for (Iterator<String> iter = handshake.iterateHttpFields(); iter.hasNext();) {
			String field = iter.next();
			j.addProperty(field, handshake.getFieldValue(field));
		}
		return j;
	}

	/**
	 * Get field from cookie in the handshake.
	 *
	 * @param handshake the Handshake
	 * @param fieldname the field name
	 * @return value as optional
	 */
	protected static Optional<String> getFieldFromHandshakeCookie(ClientHandshake handshake, String fieldname) {
		JsonObject jCookie = new JsonObject();
		if (handshake.hasFieldValue("cookie")) {
			String cookieString = handshake.getFieldValue("cookie");
			for (String cookieVariable : cookieString.split("; ")) {
				String[] keyValue = cookieVariable.split("=");
				if (keyValue.length == 2) {
					jCookie.addProperty(keyValue[0], keyValue[1]);
				}
			}
		}
		return JsonUtils.getAsOptionalString(jCookie, fieldname);
	}
}
