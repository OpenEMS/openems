package io.openems.femsserver.browserwebsocket;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.femsserver.influx.Influxdb;
import io.openems.femsserver.odoo.Odoo;
import io.openems.femsserver.utilities.OpenemsException;

public class BrowserWebsocket extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(BrowserWebsocket.class);
	private final ConcurrentHashMap<WebSocket, BrowserConnection> sockets = new ConcurrentHashMap<>();
	private final Odoo odoo;
	private final Influxdb influxdb;

	public BrowserWebsocket(int port) throws Exception {
		super(new InetSocketAddress(port));
		this.odoo = Odoo.getInstance();
		this.influxdb = Influxdb.getInstance();
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("Close connection to [" + conn + "]" //
				+ " Code [" + code + "] Reason [" + reason + "]");
		sockets.remove(conn);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.info("Error on connection to [" + conn + "]: " + ex.getMessage());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		log.info("onMessage: " + conn + ", " + message);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("onOpen: " + conn + ", " + handshake);

		// try to read session_id
		String sessionId = parseSessionId(handshake);
		log.info("Session-ID: " + sessionId);
		if (sessionId != null) {
			try {
				BrowserConnection bc = new BrowserConnection(sessionId);
				sockets.put(conn, bc);
			} catch (OpenemsException e) {
				log.error(e.getMessage());
				// TODO Auto-generated catch block
				conn.send("Authentication failed");
				try {
					Thread.sleep(1000); // give some time to send data
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				conn.close();
			}
		} else {
			conn.close();
		}
	}

	/**
	 * Tries to find a "session_id" in the handshake
	 * 
	 * @param handshake
	 * @return session_id or null
	 */
	private String parseSessionId(ClientHandshake handshake) {
		String sessionId = null;
		if (handshake.hasFieldValue("cookie")) {
			String cookieString = handshake.getFieldValue("cookie");
			for (String cookieVariable : cookieString.split(" ")) {
				String[] keyValue = cookieVariable.split("=");
				if (keyValue.length == 2 && keyValue[0].equals("session_id")) {
					sessionId = keyValue[1];
				}
			}
		}
		return sessionId;
	}
}
