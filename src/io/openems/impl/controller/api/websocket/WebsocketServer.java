/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.api.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.core.ClassRepository;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.websocket.AuthenticatedWebsocketHandler;

public class WebsocketServer extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(WebsocketServer.class);

	private final static ConcurrentHashMap<WebSocket, AuthenticatedWebsocketHandler> websockets = new ConcurrentHashMap<>();
	private final ThingRepository thingRepository;
	private final ClassRepository classRepository;
	private final WebsocketApiController controller;

	public WebsocketServer(WebsocketApiController controller, int port) {
		super(new InetSocketAddress(port));
		this.thingRepository = ThingRepository.getInstance();
		this.classRepository = ClassRepository.getInstance();
		this.controller = controller;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("User[" + getUserName(conn) + "]: close connection." //
				+ " Code [" + code + "] Reason [" + reason + "]");
		websockets.remove(conn);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.info("User[" + getUserName(conn) + "]: error on connection. " + ex);
	}

	@Override
	public void onMessage(WebSocket websocket, String message) {
		AuthenticatedWebsocketHandler handler = websockets.get(websocket);
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
		handler.onMessage(jMessage);

		if (!handler.authenticationIsValid()) {
			websockets.remove(websocket);
		}
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("Incoming connection...");
		websockets.put(conn, new AuthenticatedWebsocketHandler(conn));
	}

	/**
	 * Gets the user name of this user, avoiding null
	 *
	 * @param conn
	 * @return
	 */
	private String getUserName(WebSocket conn) {
		if (conn == null) {
			return "NOT_CONNECTED";
		}
		AuthenticatedWebsocketHandler handler = websockets.get(conn);
		if (handler == null) {
			return "NOT_CONNECTED";
		} else {
			return handler.getUserName();
		}
	}

	/**
	 * Returns true if at least one websocket connection is existing; otherwise false
	 *
	 * @return
	 */
	public static boolean isConnected() {
		return !websockets.isEmpty();
	}

	/**
	 * Send a message to all connected websockets
	 *
	 * @param string
	 * @param timestamp
	 *
	 * @param jMessage
	 */
	public static void broadcastLog(long timestamp, String level, String source, String message) {
		websockets.forEach((websocket, handler) -> {
			if (handler.authenticationIsValid()) {
				handler.sendLog(timestamp, level, source, message);
			}
		});
	}

	// public static void sendToAll(NotificationType type, String message) {
	// websockets.forEach((websocket, handler) -> {
	// handler.send(jMessage);
	// });
	// }

}
