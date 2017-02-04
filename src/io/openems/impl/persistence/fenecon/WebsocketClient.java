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
package io.openems.impl.persistence.fenecon;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.core.utilities.websocket.WebsocketHandler;

public class WebsocketClient extends org.java_websocket.client.WebSocketClient {

	private static Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final WebsocketHandler websocketHandler;

	public WebsocketClient(URI uri, String apikey) throws Exception {
		super( //
				uri, //
				new Draft_10(), //
				Stream.of(new SimpleEntry<>("apikey", apikey))
						.collect(Collectors.toMap((se) -> se.getKey(), (se) -> se.getValue())),
				0);
		if (uri.toString().startsWith("wss")) {
			try {
				SSLContext sslContext = null;
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, null);
				this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
			} catch (NoSuchAlgorithmException | KeyManagementException e) {
				throw new Exception("Could not initialize SSL connection");
			}
		}
		this.websocketHandler = new WebsocketHandler(this.getConnection());
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		log.info("Websocket closed. Code[" + code + "] Reason[" + reason + "]");
	}

	@Override
	public void onError(Exception ex) {
		log.warn("Websocket error: " + ex.getMessage());
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		log.info("Websocket opened");
	}

	/**
	 * Message event of websocket. Forwards a new message to the handler.
	 */
	@Override
	public void onMessage(String message) {
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
		this.websocketHandler.onMessage(jMessage);
	}

	@Override
	public boolean connectBlocking() throws InterruptedException {
		boolean connected = super.connectBlocking();
		if (connected) {
			this.websocketHandler.sendConnectionSuccessfulReply();
		}
		return connected;
	}

	/**
	 * Gets the websocketHandler
	 *
	 * @return
	 */
	public WebsocketHandler getWebsocketHandler() {
		return this.websocketHandler;
	}
}
