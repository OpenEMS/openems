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
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

public class WebsocketClient extends org.java_websocket.client.WebSocketClient {

	private static Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final EdgeWebsocketHandler websocketHandler; // TODO remove

	public WebsocketClient(URI uri, String apikey) throws Exception {
		super( //
				uri, //
				new Draft_6455(), //
				Stream.of(new SimpleEntry<>("apikey", apikey))
						.collect(Collectors.toMap((se) -> se.getKey(), (se) -> se.getValue())),
				0);
		log.info("Start new websocket connection to [" + uri + "]");
		if (uri.getScheme().toString().equals("wss")) {
			// try {
			// SSLContext sslContext = null;
			// sslContext = SSLContext.getInstance("TLS");
			// sslContext.init(null, null, null);
			// this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
			// } catch (NoSuchAlgorithmException | KeyManagementException e) {
			// throw new Exception("Could not initialize SSL connection");
			// }
			this.setSocket(SSLSocketFactory.getDefault().createSocket());
		}
		this.websocketHandler = new EdgeWebsocketHandler(this.getConnection());
	}

	@Override
	public final void onClose(int code, String reason, boolean remote) {
		this.close();
		log.info("Websocket [" + this.getURI().toString() + "] closed. Code[" + code + "] Reason[" + reason + "]");
	}

	@Override
	public final void onError(Exception ex) {
		this.close();
		log.warn("Websocket [" + this.getURI().toString() + "] error: " + ex);
	}

	@Override
	public final void onOpen(ServerHandshake handshakedata) {
		log.info("Websocket [" + this.getURI().toString() + "] opened");
	}

	/**
	 * Message event of websocket. Forwards a new message to the handler.
	 */
	@Override
	public final void onMessage(String message) {
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
		this.websocketHandler.onMessage(jMessage);
	}

	private CountDownLatch connectLatch = new CountDownLatch(1);

	/**
	 * Same as connect but blocks until the websocket connected or failed to do so.
	 * Returns whether it succeeded or not.
	 *
	 * Overrides original method to be able to use custom timeout
	 */
	public boolean connectBlocking(long timeoutSeconds) throws InterruptedException {
		boolean connected = false;
		connect();
		for (int i = 0; i < timeoutSeconds && !connected; i++) {
			connectLatch.await(1, TimeUnit.SECONDS);
			connected = getConnection().isOpen();
		}
		return connected;
	}

	/**
	 * Gets the websocketHandler
	 *
	 * @return
	 */
	public EdgeWebsocketHandler getWebsocketHandler() {
		return this.websocketHandler;
	}
}
