package io.openems.femsserver.websocket;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

	private volatile String fems = null;

	public Websocket(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info(getFemsName() + ": Websocket closed. Code[" + code + "] Reason[" + reason + "]");
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		log.error(getFemsName() + ": Websocket error: " + ex.getMessage());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		JsonObject j = (new JsonParser()).parse(message).getAsJsonObject();
		try {
			/*
			 * Handle authentication
			 */
			if (j.has("authenticate")) {
				String apikey = JsonUtils.getAsString(j, "authenticate");
				Odoo odoo = Odoo.getInstance();
				try {
					String fems = odoo.getDeviceForApikey(apikey);
					this.fems = fems;
					log.info("Authenticated as " + getFemsName());
				} catch (Exception e) {
					log.error("Unable to authenticate: ", e);
				}
			}
			/*
			 * Handle data
			 */
			String fems = this.fems;
			if (fems == null) {
				log.warn("FEMS not authenticated yet.");
			} else {
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
			}
		} catch (OpenemsException e) {
			log.error("onMessage-Error: (" + fems + ")" + e.getMessage());
		}
	}

	String getFemsName() {
		return fems != null ? "fems" + fems : "UNKNOWN";
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.error(getFemsName() + ": Websocket opened");
	}
}
