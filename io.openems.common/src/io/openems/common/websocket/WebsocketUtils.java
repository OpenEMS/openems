package io.openems.common.websocket;

import java.util.Iterator;

import org.java_websocket.handshake.Handshakedata;

import com.google.gson.JsonObject;

public class WebsocketUtils {

	/**
	 * Converts a Handshake to a JsonObject
	 * 
	 * @param handshake
	 * @return
	 */
	public static JsonObject handshakeToJsonObject(Handshakedata handshake) {
		JsonObject j = new JsonObject();
		for (Iterator<String> iter = handshake.iterateHttpFields(); iter.hasNext();) {
			String field = iter.next();
			j.addProperty(field, handshake.getFieldValue(field));
		}
		return j;
	}

}
