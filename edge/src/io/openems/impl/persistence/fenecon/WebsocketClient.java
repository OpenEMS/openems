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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Config;
import io.openems.core.ConfigFormat;

public class WebsocketClient extends org.java_websocket.client.WebSocketClient {

	private static Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final FeneconPersistenceWebsocketHandler websocketHandler; // TODO remove

	public WebsocketClient(URI uri, String apikey) throws Exception {
		super( //
				uri, //
				new Draft_10(), //
				Stream.of(new SimpleEntry<>("apikey", apikey))
						.collect(Collectors.toMap((se) -> se.getKey(), (se) -> se.getValue())),
				0);
		log.info("Start new websocket connection to [" + uri.getPath() + "]");
		if (uri.toString().startsWith("wss")) {
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
		this.websocketHandler = new FeneconPersistenceWebsocketHandler(this.getConnection());
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		this.close();
		log.info("Websocket [" + this.getURI().toString() + "] closed. Code[" + code + "] Reason[" + reason + "]");
	}

	@Override
	public void onError(Exception ex) {
		this.close();
		log.warn("Websocket [" + this.getURI().toString() + "] error: " + ex);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		log.info("Websocket [" + this.getURI().toString() + "] opened");
	}

	/**
	 * Message event of websocket. Forwards a new message to the handler.
	 */
	@Override
	public void onMessage(String message) {
		JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
		// this.websocketHandler.onMessage(jMessage);
		log.info(jMessage.toString());

		// get message id -> used for reply
		Optional<JsonArray> jIdOpt = JsonUtils.getAsOptionalJsonArray(jMessage, "id");
		log.info("Message-ID: " + jIdOpt);

		// prepare reply (every reply is going to be merged into this object)
		Optional<JsonObject> jReplyOpt = Optional.empty();

		/*
		 * Config
		 */
		Optional<JsonObject> jConfig = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
		if (jConfig.isPresent()) {
			jReplyOpt = JsonUtils.merge(jReplyOpt, //
					config(jConfig.get()) //
			);
		}

		// send reply
		if (jReplyOpt.isPresent()) {
			JsonObject jReply = jReplyOpt.get();
			if (jIdOpt.isPresent()) {
				jReply.add("id", jIdOpt.get());
			}
			WebSocketUtils.send(this.getConnection(), jReply);
		}
	}

	/**
	 * Handle "config" messages
	 *
	 * @param jConfig
	 * @return
	 */
	private synchronized Optional<JsonObject> config(JsonObject jConfig) {
		try {
			String mode = JsonUtils.getAsString(jConfig, "mode");

			if (mode.equals("query")) {
				/*
				 * Query current config
				 */
				String language = JsonUtils.getAsString(jConfig, "language");
				JsonObject jReplyConfig = Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, language);
				return Optional.of(DefaultMessages.configQueryReply(jReplyConfig));
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
		return Optional.empty();
	}

	private CountDownLatch connectLatch = new CountDownLatch(1);

	/**
	 * Same as connect but blocks until the websocket connected or failed to do so.
	 * Returns whether it succeeded or not.
	 *
	 * Overrides original method to be able to use custom timeout
	 */
	public boolean connectBlocking(long timeoutSeconds) throws InterruptedException {
		connect();
		connectLatch.await(timeoutSeconds, TimeUnit.SECONDS);
		boolean connected = getConnection().isOpen();
		return connected;
	}

	/**
	 * Gets the websocketHandler
	 *
	 * @return
	 */
	public FeneconPersistenceWebsocketHandler getWebsocketHandler() {
		return this.websocketHandler;
	}
}
