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

import java.util.ArrayList;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.impl.controller.api.websocket.session.WebsocketApiSession;
import io.openems.impl.controller.api.websocket.session.WebsocketApiSessionData;
import io.openems.impl.controller.api.websocket.session.WebsocketApiSessionManager;

public class WebsocketApiServer
		extends AbstractWebsocketServer<WebsocketApiSession, WebsocketApiSessionData, WebsocketApiSessionManager> {

	private static Logger log = LoggerFactory.getLogger(WebsocketApiServer.class);

	private final WebsocketApiController controller;

	public WebsocketApiServer(WebsocketApiController controller, int port) {
		super(port, new WebsocketApiSessionManager());
		this.controller = controller;
	}

	/**
	 * Open event of websocket. Parses the Odoo "session_id" and stores it in a new Session.
	 */
	@Override
	public void onOpen(WebSocket websocket, ClientHandshake handshake) {
		JsonObject jHandshake = this.parseCookieFromHandshake(handshake);
		Optional<String> tokenOpt = JsonUtils.getAsOptionalString(jHandshake, "token");
		if (tokenOpt.isPresent()) {
			String token = tokenOpt.get();
			Optional<WebsocketApiSession> sessionOpt = this.sessionManager.getSessionByToken(token);
			if (sessionOpt.isPresent()) {
				WebsocketApiSession session = sessionOpt.get();
				WebSocket oldWebsocket = session.getData().getWebsocketHandler().getWebsocket();
				if (oldWebsocket != null) {
					// TODO to avoid this, websockets needs to be able to handle more than one websocket per session
					oldWebsocket.closeConnection(CloseFrame.REFUSE, "Another client connected with this token");
				}
				// refresh session
				session.getData().getWebsocketHandler().setWebsocket(websocket);
				// add to websockets
				this.websockets.forcePut(websocket, session);
				// send connection successful to browser
				JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(),
						Optional.of(session.getData().getRole()), new ArrayList<>());
				log.info("Websocket [" + websocket + "] connected by session. User [" + session.getData().getUser()
						+ "] Session [" + session.getToken() + "]");
				WebSocketUtils.send(websocket, jReply);
				return;
			}
		}
		// if we are here, automatic authentication was not possible -> notify client
		WebSocketUtils.send(websocket,
				DefaultMessages.notification(Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED, tokenOpt.orElse("")));
	}

	@Override
	protected void onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		// log.info("RECV: websocket[" + websocket + "]" + jMessage.toString());

		/*
		 * Authenticate
		 */
		Optional<WebsocketApiSession> sessionOpt = Optional.empty();
		if (jMessage.has("authenticate")) {
			// authenticate by username/password
			sessionOpt = authenticate(jMessage.get("authenticate"), websocket);
		}
		if (!sessionOpt.isPresent()) {
			// check if there is an existing session
			sessionOpt = Optional.ofNullable(this.websockets.get(websocket));
		}
		if (!sessionOpt.isPresent()) {
			/*
			 * send authentication failed reply
			 */
			JsonObject jReply = DefaultMessages.browserConnectionFailedReply();
			WebSocketUtils.send(websocket, jReply);
			websocket.closeConnection(CloseFrame.REFUSE, "Authentication failed");
			return;
		}
		WebsocketApiSession session = sessionOpt.get();
		/*
		 * On initial authentication...
		 */
		if (jMessage.has("authenticate")) {
			// add to websockets
			this.websockets.put(websocket, session);
			// send connection successful to browser
			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(),
					Optional.of(session.getData().getRole()), new ArrayList<>());
			log.info("Browser connected by authentication. User [" + session.getData().getUser() + "] Session ["
					+ session.getToken() + "]");
			WebSocketUtils.send(websocket, jReply);
		}

		/*
		 * Rest -> forward to websocket handler
		 */
		session.getData().getWebsocketHandler().onMessage(jMessage);
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Stores the session if valid.
	 *
	 * @param jAuthenticateElement
	 * @param handler
	 */
	private Optional<WebsocketApiSession> authenticate(JsonElement jAuthenticateElement, WebSocket websocket) {
		try {
			JsonObject jAuthenticate = JsonUtils.getAsJsonObject(jAuthenticateElement);
			if (jAuthenticate.has("mode")) {
				String mode = JsonUtils.getAsString(jAuthenticate, "mode");
				if (mode.equals("login")) {
					if (jAuthenticate.has("password")) {
						/*
						 * Authenticate using username and password
						 */
						String password = JsonUtils.getAsString(jAuthenticate, "password");
						if (jAuthenticate.has("username")) {
							String username = JsonUtils.getAsString(jAuthenticate, "username");
							return this.sessionManager.authByUserPassword(username, password, websocket);
						} else {
							return this.sessionManager.authByPassword(password, websocket);
						}
					}
				}
			}
		} catch (OpenemsException e) { /* ignore */ }
		return Optional.empty();
	}

	/**
	 * Send a log entry to all connected websockets
	 *
	 * @param string
	 * @param timestamp
	 *
	 * @param jMessage
	 */
	public void broadcastLog(long timestamp, String level, String source, String message) {
		for (WebsocketApiSession session : this.sessionManager.getSessions()) {
			session.getData().getWebsocketHandler().sendLog(timestamp, level, source, message);
		}
	}
}
