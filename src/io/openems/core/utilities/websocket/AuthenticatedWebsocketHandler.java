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
package io.openems.core.utilities.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.exception.ReflectionException;
import io.openems.api.security.Authentication;
import io.openems.api.security.Session;
import io.openems.core.utilities.JsonUtils;
import io.openems.impl.controller.api.websocket.WebsocketApiController;

/**
 * Extends {@link WebsocketHandler} with authentication functionality, like session token
 *
 * @author stefan.feilmeier
 *
 */
public class AuthenticatedWebsocketHandler extends WebsocketHandler {

	private static Logger log = LoggerFactory.getLogger(AuthenticatedWebsocketHandler.class);

	/**
	 * Holds the authenticated session
	 */
	private Session session = null;

	public AuthenticatedWebsocketHandler(WebSocket websocket, WebsocketApiController controller) {
		super(websocket, controller);
	}

	public boolean authenticationIsValid() {
		if (this.session != null && this.session.isValid()) {
			return true;
		}
		return false;
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public void onMessage(JsonObject jMessage) {
		// log.info(jMessage.toString());
		/*
		 * Authenticate user and send immediate reply
		 */
		if (jMessage.has("authenticate")) {
			authenticate(jMessage.get("authenticate"));
		}

		/*
		 * Check authentication
		 */
		if (!authenticationIsValid()) {
			// no user authenticated till now -> exit
			this.websocket.close();
			return;
		}

		/*
		 * Send message on initial call
		 */
		if (jMessage.has("authenticate")) {
			sendConnectionSuccessfulReply();
		}

		/*
		 * Rest -> forward to super class
		 */
		super.onMessage(jMessage);
	}

	/**
	 * Authenticates a user according to the "authenticate" message. Stores the session if valid.
	 *
	 * @param jAuthenticateElement
	 * @param handler
	 */
	private void authenticate(JsonElement jAuthenticateElement) {
		Authentication auth = Authentication.getInstance();
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
							this.session = auth.byUserPassword(username, password);
						} else {
							this.session = auth.byPassword(password);
						}
					} else if (jAuthenticate.has("token")) {
						/*
						 * Authenticate using session token
						 */
						String token = JsonUtils.getAsString(jAuthenticate, "token");
						this.session = auth.bySession(token);
					}
				}
			}
		} catch (ReflectionException e) { /* ignore */ }
	}

	/**
	 * Creates an initial message to the browser after it was successfully connected and authenticated
	 *
	 * <pre>
	 * {
	 *   authenticate: {
	 *     mode: "allow",
	 *     [token: "...",]
	 *     [username: "..."]
	 *   }, metadata: {
	 *     devices: [{
	 *       name: {...},
	 *       config: {...}
	 *       online: true
	 *     }],
	 *     backend: "openems"
	 *   }
	 * }
	 * </pre>
	 *
	 * @param handler
	 */
	@Override
	protected JsonObject createConnectionSuccessfulReply() {
		JsonObject j = super.createConnectionSuccessfulReply();

		// Authentication data
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "allow");
		jAuthenticate.addProperty("username", this.session.getUser().getName());
		jAuthenticate.addProperty("token", this.session.getToken());
		j.add("authenticate", jAuthenticate);

		return j;
	}

	/**
	 * Gets the user name of this user, avoiding null
	 *
	 * @param conn
	 * @return
	 */
	public String getUserName() {
		if (session != null && session.getUser() != null) {
			return session.getUser().getName();
		}
		return "NOT_CONNECTED";
	}
}
