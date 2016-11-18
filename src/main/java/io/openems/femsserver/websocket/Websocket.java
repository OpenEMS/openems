package io.openems.femsserver.websocket;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.influx.Influxdb;
import io.openems.femsserver.odoo.Odoo;
import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;

public class Websocket extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(Websocket.class);
	private final ConcurrentHashMap<WebSocket, String> sockets = new ConcurrentHashMap<>();
	private final Odoo odoo;

	public Websocket(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.odoo = Odoo.getInstance();
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("Close connection to [" + this.getFemsName(conn) + "]" //
				+ " Code [" + code + "] Reason [" + reason + "]");
		sockets.remove(conn);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.info("Error on connection to [" + this.getFemsName(conn) + "]: " + ex.getMessage());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		String fems = sockets.get(conn);
		if (fems == null) {
			// ignore
			return;
		}
		JsonObject j = (new JsonParser()).parse(message).getAsJsonObject();
		try {
			/*
			 * Handle data
			 */
			if (j.has("data") || j.has("cachedData")) {
				JsonObject jData;
				if (j.has("data")) {
					jData = JsonUtils.getAsJsonObject(j, "data");
				} else { // cachedData
					jData = JsonUtils.getAsJsonObject(j, "cachedData");
				}
				try {
					Influxdb influxdb = Influxdb.getInstance();
					influxdb.write(fems, jData);
				} catch (Exception e) {
					log.error("No InfluxDB-connection: ", e);
				}
			}
		} catch (OpenemsException e) {
			log.error("Error on message from [" + fems + "]: " + e.getMessage());
		}
	}

	String getFemsName(WebSocket conn) {
		String fems = sockets.get(conn);
		return fems != null ? "fems" + fems : "UNKNOWN";
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		String fems = null;
		try {
			if (handshake.hasFieldValue("apikey")) {
				String apikey = handshake.getFieldValue("apikey");
				fems = odoo.getDeviceForApikey(apikey);
				log.info("Incoming connection from [" + fems + "]");
				sockets.put(conn, fems);
			} else {
				throw new Exception("Apikey is missing");
			}
		} catch (Exception e) {
			log.warn("Connection failed: " + e.getMessage());
			conn.close();
		}
	}
}
