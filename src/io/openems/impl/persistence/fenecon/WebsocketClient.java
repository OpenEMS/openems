package io.openems.impl.persistence.fenecon;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class WebsocketClient extends org.java_websocket.client.WebSocketClient {

	private static Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final String apikey;

	public WebsocketClient(URI uri, String apikey) throws Exception {
		super(uri);
		this.apikey = apikey;

		if (uri.toString().startsWith("wss")) {
			log.info("Socket: Using SSL");
			try {
				SSLContext sslContext = null;
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, null);
				this.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
			} catch (NoSuchAlgorithmException | KeyManagementException e) {
				throw new Exception("Could not initialize SSL connection");
			}
		}
	}

	@Override public void onClose(int code, String reason, boolean remote) {
		log.info("Websocket closed. Code[" + code + "] Reason[" + reason + "]");
		log.info(this.getDraft().toString());
	}

	@Override public void onError(Exception ex) {
		log.warn("Websocket error: " + ex.getMessage());
	}

	@Override public void onMessage(String message) {
		log.info("Websocket message: " + message);
	}

	@Override public void onOpen(ServerHandshake handshakedata) {
		log.info("Websocket opened");
		// send authentication
		JsonObject j = new JsonObject();
		j.addProperty("authenticate", apikey);
		send(j);
	}

	public boolean send(JsonObject j) {
		try {
			send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			return false;
		}
	}
}
