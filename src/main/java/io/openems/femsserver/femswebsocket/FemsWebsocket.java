package io.openems.femsserver.femswebsocket;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.influx.Influxdb;
import io.openems.femsserver.odoo.Odoo;
import io.openems.femsserver.odoo.fems.device.FemsDevice;
import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;

public class FemsWebsocket extends WebSocketServer {

	private static Logger log = LoggerFactory.getLogger(FemsWebsocket.class);
	private final ConcurrentHashMap<WebSocket, FemsDevice> sockets = new ConcurrentHashMap<>();
	private final Odoo odoo;
	private final Influxdb influxdb;

	public FemsWebsocket(int port) throws Exception {
		super(new InetSocketAddress(port));
		this.odoo = Odoo.getInstance();
		this.influxdb = Influxdb.getInstance();
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
		FemsDevice fems = sockets.get(conn);
		if (fems == null) {
			// ignore
			return;
		}
		fems.setLastMessage();
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
				/*
				 * Write to InfluxDB
				 */
				try {
					influxdb.write(fems.getNameNumber(), jData);
				} catch (Exception e) {
					log.error("No InfluxDB-connection: ", e);
				}
				/*
				 * Write some data to Odoo
				 */
				// TODO: this is only to provide feedback for FENECON
				// Service-Team that the device is online. Replace with
				// something based on the actual websocket connection
				if (j.has("data")) {
					// take only not cached data
					fems.setLastUpdate();
					for (Entry<String, JsonElement> timestampEntry : JsonUtils.getAsJsonObject(j, "data").entrySet()) {
						JsonObject jChannels = JsonUtils.getAsJsonObject(timestampEntry.getValue());
						if (jChannels.has("ess0/Soc")) {
							int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
							fems.setSoc(soc);
						}						
						if (jChannels.has("system0/PrimaryIpAddress")) {
							String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
							fems.setIpV4(ipv4);
						}
					}
				}
			}
		} catch (OpenemsException e) {
			log.error("Error on message from [" + fems.getName() + "]: " + e.getMessage());
		}
		try {
			fems.writeObject();
		} catch (Exception e) {
			log.error(fems.getName() + ": Updating Odoo failed: " + e.getMessage());
		}
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		FemsDevice fems = null;
		try {
			if (handshake.hasFieldValue("apikey")) {
				String apikey = handshake.getFieldValue("apikey");
				fems = odoo.getFirstDeviceForApikey(apikey);
				if (fems == null) {
					throw new Exception("Unable to find device from apikey [" + apikey + "]");
				}
				log.info("Incoming connection from [" + fems.getName() + "]");
				sockets.put(conn, fems);
			} else {
				throw new Exception("Apikey is missing");
			}
		} catch (Exception e) {
			log.warn("Connection failed: " + e.getMessage());
			conn.close();
		}
	}

	private String getFemsName(WebSocket conn) {
		FemsDevice device = sockets.get(conn);
		if (device == null) {
			return "NOT_CONNECTED";
		} else {
			return device.getName();
		}
	}
}
