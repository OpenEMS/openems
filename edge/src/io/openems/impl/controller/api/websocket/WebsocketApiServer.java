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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.security.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.utilities.api.ApiWorker;

public class WebsocketApiServer extends AbstractWebsocketServer {

	private static Logger log = LoggerFactory.getLogger(WebsocketApiServer.class);
	private final Map<String, UiEdgeWebsocketHandler> handlers = new HashMap<>();
	private final ApiWorker apiWorker;
	private final static int TOKEN_LENGTH = 130;

	public WebsocketApiServer(ApiWorker apiWorker, int port) {
		super(port);
		this.apiWorker = apiWorker;
	}

	/**
	 * Open event of websocket.
	 */
	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		// login using token from the cookie
		Optional<String> tokenOpt =  getFieldFromHandshakeCookie(handshake, "token");
		if (tokenOpt.isPresent()) {
			String token = tokenOpt.get();
			UiEdgeWebsocketHandler handler = this.handlers.get(token);
			if (handler != null) {
				try {
					// Authentication successful
					this.handleAuthenticationSuccessful(handler.getUser(), websocket);
					log.info("User [" + getUserName(websocket) + "] logged in by token");
					return;
				} catch (OpenemsException e) {
					// TODO handle error
					log.error(e.getMessage());
				}
			}
		}

		// if we are here, automatic authentication was not possible -> notify client
		WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
				LogBehaviour.WRITE_TO_LOG, Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED, tokenOpt.orElse(""));
	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage) {
		/*
		 * Authenticate
		 */
		Optional<JsonObject> jAuthenticateOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "authenticate");
		if (jAuthenticateOpt.isPresent()) {
			// authenticate by username/password
			try {
				authenticate(jAuthenticateOpt.get(), websocket);
			} catch (OpenemsException e) {
				// TODO error
				log.error(e.getMessage());
			}
			return;
		}

		// get current Token
		String token = websocket.getAttachment();
		if (token == null) {
			// TODO error: no token
			return;
		}
		UiEdgeWebsocketHandler handler = this.handlers.get(token);
		if (handler == null) {
			// TODO error: no handler
			return;
		}

		/*
		 * Rest -> forward to websocket handler
		 */
		handler.onMessage(jMessage);
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		log.info("User [" + getUserName(websocket) + "] closed websocket connection");
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Stores and returnes the Role if valid.
	 *
	 * @param jAuthenticateElement
	 * @param handler
	 * @throws OpenemsException
	 */
	private void authenticate(JsonObject jAuthenticate, WebSocket websocket) throws OpenemsException {
		if (jAuthenticate.has("mode")) {
			String mode = JsonUtils.getAsString(jAuthenticate, "mode");
			switch (mode) {
			case "login":
				try {
					/*
					 * Authenticate using password (and optionally username)
					 */
					String password = JsonUtils.getAsString(jAuthenticate, "password");
					Optional<String> usernameOpt = JsonUtils.getAsOptionalString(jAuthenticate, "username");
					Optional<User> userOpt;
					if (usernameOpt.isPresent()) {
						userOpt = User.authenticate(usernameOpt.get(), password);
					} else {
						userOpt = User.authenticate(password);
					}

					if (!userOpt.isPresent()) {
						throw new OpenemsException("Authentication failed");
					}
					// authentication successful
					User user = userOpt.get();
					log.info("User [" + user.getName() + "] logged in by username/password");
					this.handleAuthenticationSuccessful(user, websocket);

				} catch (OpenemsException e) {
					/*
					 * send authentication failed reply
					 */
					JsonObject jReply = DefaultMessages.uiConnectionFailedReply();
					WebSocketUtils.send(websocket, jReply);
					websocket.closeConnection(CloseFrame.REFUSE, "Error while authenticating: " + e.getMessage());
					return;
				}
				break;
			case "logout":
				/*
				 * Logout and close session
				 */
				String token = websocket.getAttachment();
				if (token != null) {
					UiEdgeWebsocketHandler handler = this.handlers.get(token);
					if (handler != null) {
						handler.dispose();
					}
					this.handlers.remove(token);
					log.info("User [" + getUserName(websocket) + "] logged out. Invalidated token [" + token + "]");
					// TODO send notification
				} else {
					log.warn("User tries to log out, but was not logged in.");
				}
				websocket.setAttachment(null);
			}
		}

	}

	private void handleAuthenticationSuccessful(User user, WebSocket websocket) throws OpenemsException {
		// Create Edges entry
		JsonObject jEdge = new JsonObject();
		jEdge.addProperty("id", 0);
		jEdge.addProperty("name", "fems0");
		jEdge.addProperty("comment", "FEMS");
		jEdge.addProperty("producttype", "");
		jEdge.addProperty("role", user.getRole().toString().toLowerCase());
		jEdge.addProperty("online", true);
		JsonArray jEdges = new JsonArray();
		jEdges.add(jEdge);

		// invalidate old handler if exists
		String token = websocket.getAttachment();
		if (token != null) {
			UiEdgeWebsocketHandler oldHandler = this.handlers.get(token);
			oldHandler.dispose();
			this.handlers.remove(token);
		} else {
			// Generate token (source: http://stackoverflow.com/a/41156)
			SecureRandom sr = SecureRandomSingleton.getInstance();
			token = new BigInteger(TOKEN_LENGTH, sr).toString(32);
		}

		// create new Handler
		UiEdgeWebsocketHandler handler = new UiEdgeWebsocketHandler(token, user);

		// save
		this.handlers.put(token, handler);
		websocket.setAttachment(token);

		// send reply
		JsonObject jReply = DefaultMessages.uiConnectionSuccessfulReply(token, jEdges);
		WebSocketUtils.send(websocket, jReply);
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.warn("User [" + getUserName(websocket) + "] error: " + ex.getMessage());
	}

	/**
	 * Get token from handshake
	 *
	 * @param handshake
	 * @return cookie as JsonObject
	 */
	private Optional<String> getTokenFromHandshake(ClientHandshake handshake) {
		if (handshake.hasFieldValue("token")) {
			return Optional.ofNullable(handshake.getFieldValue("token"));
		}
		return Optional.empty();
	}

	private String getUserName(WebSocket websocket) {
		String token = websocket.getAttachment();
		if (token != null) {
			UiEdgeWebsocketHandler handler = this.handlers.get(token);
			if(handler != null) {
				User user = handler.getUser();
				return user.getName();
			}
		}
		return "UNKNOWN";
	}

	public void sendLog(long timestamp, String level, String source, String message) {
		for(UiEdgeWebsocketHandler handler : this.handlers.values()) {
			handler.sendLog(timestamp, level, source, message);
		}
	}
}
