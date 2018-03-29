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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.security.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Config;
import io.openems.core.ConfigFormat;
import io.openems.core.utilities.api.ApiWorker;

public class WebsocketApiServer extends AbstractWebsocketServer {

	private final static Logger log = LoggerFactory.getLogger(WebsocketApiServer.class);

	/**
	 * Stores valid session tokens for authentication via Cookie (this maps to a browser window)
	 */
	private final Map<String, User> sessionTokens = new HashMap<>();

	/**
	 * Stores handlers per websocket (this maps to a browser tab).
	 * The handler lives while the websocket is connected. Independently of the login/logout state.
	 */
	private final Map<UUID, UiEdgeWebsocketHandler> handlers = new HashMap<>();
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
		// generate UUID for this websocket (browser tab)
		UUID uuid = UUID.randomUUID();

		// get token from cookie or generate new token
		String token;
		Optional<String> cookieTokenOpt = getFieldFromHandshakeCookie(handshake, "token");
		if (cookieTokenOpt.isPresent()) {
			token = cookieTokenOpt.get();
		} else {
			// Generate token (source: http://stackoverflow.com/a/41156)
			SecureRandom sr = SecureRandomSingleton.getInstance();
			token = new BigInteger(TOKEN_LENGTH, sr).toString(32);
		}

		// create new Handler and store it
		UiEdgeWebsocketHandler handler = new UiEdgeWebsocketHandler(websocket, apiWorker, token, uuid);
		this.handlers.put(uuid, handler);
		websocket.setAttachment(uuid);

		// login using token from the cookie
		if (cookieTokenOpt.isPresent()) {
			User user = this.sessionTokens.get(token);
			if (user != null) {
				/*
				 * token from cookie is valid -> authentication successful
				 */
				// send reply and log
				try {
					this.handleAuthenticationSuccessful(handler, user);
					log.info("User [" + user.getName() + "] logged in by token");
					return;
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
							LogBehaviour.WRITE_TO_LOG, Notification.ERROR, e.getMessage());
				}
			}
		}

		// if we are here, automatic authentication was not possible -> notify client
		WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
				LogBehaviour.WRITE_TO_LOG, Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED, cookieTokenOpt.orElse(""));
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Stores the User if valid.
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
					UiEdgeWebsocketHandler handler = getHandlerOrCloseWebsocket(websocket);
					this.sessionTokens.put(handler.getSessionToken(), user);
					this.handleAuthenticationSuccessful(handler, user);

				} catch (OpenemsException e) {
					/*
					 * send authentication failed reply
					 */
					JsonObject jReply = DefaultMessages.uiLogoutReply();
					WebSocketUtils.send(websocket, jReply);
					log.info(e.getMessage());
					return;
				}
				break;
			case "logout":
				/*
				 * Logout and close session
				 */
				String sessionToken = "none";
				String username = "UNKNOWN";
				try {
					UiEdgeWebsocketHandler handler = this.getHandlerOrCloseWebsocket(websocket);
					Optional<User> thisUserOpt = handler.getUserOpt();
					if (thisUserOpt.isPresent()) {
						username = thisUserOpt.get().getName();
						handler.unsetUser();
					}
					sessionToken = handler.getSessionToken();
					this.sessionTokens.remove(sessionToken);
					log.info("User [" + username + "] logged out. Invalidated token [" + sessionToken + "]");

					// find and close all websockets for this user
					if (thisUserOpt.isPresent()) {
						User thisUser = thisUserOpt.get();
						for (UiEdgeWebsocketHandler h : this.handlers.values()) {
							if (h.getUserOpt().isPresent()) {
								User otherUser = h.getUserOpt().get();
								if (otherUser.equals(thisUser)) {
									JsonObject jReply = DefaultMessages.uiLogoutReply();
									h.send(jReply);
									h.dispose();
								}
							}
						}
					}
					JsonObject jReply = DefaultMessages.uiLogoutReply();
					WebSocketUtils.send(websocket, jReply);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
							LogBehaviour.WRITE_TO_LOG, Notification.ERROR,
							"Unable to close session [" + sessionToken + "]: " + e.getMessage());
				}
			}
		}

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
				WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
						LogBehaviour.WRITE_TO_LOG, Notification.ERROR, e.getMessage());
			}
			return;
		}

		// get handler
		UiEdgeWebsocketHandler handler;
		try {
			handler = getHandlerOrCloseWebsocket(websocket);
		} catch (OpenemsException e) {
			WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
					LogBehaviour.WRITE_TO_LOG, Notification.ERROR, "onMessage Error: " + e.getMessage());
			return;
		}

		// get session Token from handler
		String token = handler.getSessionToken();
		if (!this.sessionTokens.containsKey(token)) {
			WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
					LogBehaviour.WRITE_TO_LOG, Notification.ERROR, "Token [" + token + "] is not anymore valid.");
			websocket.close();
			return;
		}

		// From here authentication was successful

		/*
		 * Rest -> forward to websocket handler
		 */
		handler.onMessage(jMessage);
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.warn("User [" + getUserName(websocket) + "] error: " + ex.getMessage());
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		log.info("User [" + getUserName(websocket) + "] closed websocket connection");
		this.disposeHandler(websocket);
	}

	private void handleAuthenticationSuccessful(UiEdgeWebsocketHandler handler, User user) throws OpenemsException {
		// add user to handler
		handler.setUser(user);

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

		// send reply
		JsonObject jReply = DefaultMessages.uiLoginSuccessfulReply(handler.getSessionToken(), jEdges);
		handler.send(jReply);
	}

	public void sendLog(long timestamp, String level, String source, String message) {
		for (UiEdgeWebsocketHandler handler : this.handlers.values()) {
			handler.sendLog(timestamp, level, source, message);
		}
	}

	private String getUserName(WebSocket websocket) {
		Optional<UiEdgeWebsocketHandler> handlerOpt = getHandlerOpt(websocket);
		if (handlerOpt.isPresent()) {
			UiEdgeWebsocketHandler handler = handlerOpt.get();
			if (handler.getUserOpt().isPresent()) {
				User user = handler.getUserOpt().get();
				return user.getName();
			}
		}
		return "UNKNOWN";
	}

	private Optional<UiEdgeWebsocketHandler> getHandlerOpt(WebSocket websocket) {
		UUID uuid = websocket.getAttachment();
		return Optional.ofNullable(this.handlers.get(uuid));
	}

	private UiEdgeWebsocketHandler getHandlerOrCloseWebsocket(WebSocket websocket) throws OpenemsException {
		Optional<UiEdgeWebsocketHandler> handlerOpt = getHandlerOpt(websocket);
		UUID uuid = websocket.getAttachment();
		UiEdgeWebsocketHandler handler = this.handlers.get(uuid);
		if (!handlerOpt.isPresent()) {
			// no handler! close websocket
			websocket.close();
			throw new OpenemsException("Websocket had no Handler. Closing websocket.");
		}
		return handler;
	}

	private void disposeHandler(WebSocket websocket) {
		UiEdgeWebsocketHandler handler;
		try {
			handler = getHandlerOrCloseWebsocket(websocket);
			UUID uuid = handler.getUuid();
			this.handlers.remove(uuid);
			handler.dispose();
		} catch (OpenemsException e) {
			log.warn("Unable to dispose Handler: " + e.getMessage());
		}
	}

	private final static String DEFAULT_CONFIG_LANGUAGE = "en";

	public void onConfigUpdate() {
		for(UiEdgeWebsocketHandler handler : this.handlers.values()) {
			try {
				Role role = handler.getUserOpt().get().getRole();
				JsonObject j = DefaultMessages.configQueryReply(new JsonObject(),
						Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, role, DEFAULT_CONFIG_LANGUAGE));
				handler.send(j);
			} catch (OpenemsException | NoSuchElementException e) {
				log.warn(e.getMessage());
			}
		}
	}
}
