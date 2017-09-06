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

import com.google.gson.JsonObject;

import io.openems.impl.controller.api.websocket.WebsocketApiController;

/**
 * Extends {@link WebsocketHandler} with authentication functionality, like session token
 *
 * @author stefan.feilmeier
 *
 */
public class AuthenticatedWebsocketHandler extends WebsocketHandler {

	public AuthenticatedWebsocketHandler(WebSocket websocket, WebsocketApiController controller) {
		super(websocket, controller);
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public void onMessage(JsonObject jMessage) {

	}

	// /**
	// * Gets the user name of this user, avoiding null
	// *
	// * @param conn
	// * @return
	// */
	// public String getUserName() {
	// if (session != null && session.getUser() != null) {
	// return session.getUser().getName();
	// }
	// return "NOT_CONNECTED";
	// }
}
