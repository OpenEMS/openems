package io.openems.backend.openemswebsocket;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.browserwebsocket.BrowserWebsocket;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.backend.openemswebsocket.session.OpenemsSession;
import io.openems.backend.openemswebsocket.session.OpenemsSessionData;
import io.openems.backend.openemswebsocket.session.OpenemsSessionManager;
import io.openems.backend.timedata.Timedata;
import io.openems.backend.utilities.StringUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections to OpenEMS-Devices.
 *
 * @author stefan.feilmeier
 *
 */
public class OpenemsWebsocketSingleton extends WebSocketServer {

	private final Logger log = LoggerFactory.getLogger(OpenemsWebsocketSingleton.class);

	private final BiMap<WebSocket, OpenemsSession> websockets = Maps.synchronizedBiMap(HashBiMap.create());
	private final OpenemsSessionManager sessionManager = new OpenemsSessionManager();

	protected OpenemsWebsocketSingleton(int port) throws Exception {
		super(new InetSocketAddress(port));
	}

	/**
	 * Open event of websocket. Parses the "apikey" and stores it in a new Session.
	 */
	@Override
	public void onOpen(WebSocket websocket, ClientHandshake handshake) {
		String apikey = "";
		String deviceName = "";
		try {
			// get apikey from handshake
			Optional<String> apikeyOpt = parseApikeyFromHandshake(handshake);
			if (!apikeyOpt.isPresent()) {
				throw new OpenemsException("Apikey is missing in handshake");
			}
			apikey = apikeyOpt.get();

			// get device for apikey
			Optional<MetadataDevice> deviceOpt = Metadata.instance().getDeviceModel().getDeviceForApikey(apikey);
			if (!deviceOpt.isPresent()) {
				throw new OpenemsException("Unable to find device for apikey [" + apikey + "]");
			}
			MetadataDevice device = deviceOpt.get();
			deviceName = device.getName();

			// create new session if no existing one was found
			OpenemsSessionData sessionData = new OpenemsSessionData(device);
			OpenemsSession session = sessionManager.createNewSession(apikey, sessionData);
			session.setValid();

			// send successful reply to openems
			JsonObject jReply = DefaultMessages.openemsConnectionSuccessfulReply();
			log.info("OpenEMS connected. Device [" + deviceName + "]");
			WebSocketUtils.send(websocket, jReply);

			// add websocket to local cache
			this.websockets.forcePut(websocket, session);

			try {
				// set device active (in Odoo)
				if (device.getState().equals("inactive")) {
					device.setState("active");
				}
				device.setLastMessage();
				device.writeObject();
			} catch (OpenemsException e) {
				// this error does not stop the connection
				log.warn(e.getMessage());
			}

		} catch (OpenemsException e) {
			// send connection failed to OpenEMS
			JsonObject jReply = DefaultMessages.openemsConnectionFailedReply(e.getMessage());
			WebSocketUtils.send(websocket, jReply);
			log.info("OpenEMS connection failed. Device [" + deviceName + "] Apikey [" + apikey + "]");

			// close websocket
			websocket.close();
		}
	}

	/**
	 * Close event of websocket. Removes the websocket.
	 */
	@Override
	public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		OpenemsSession session = this.websockets.get(websocket);
		if (session != null) {
			log.info("OpenEMS connection closed. Device [" + session.getData().getDevice().getName() + "] Code [" + code
					+ "] Reason [" + reason + "]");
		} else {
			log.info("Browser connection closed. Code [" + code + "] Reason [" + reason + "]");
		}
		sessionManager.removeSession(session);
		this.websockets.remove(websocket);
	}

	/**
	 * Error event of websocket. Logs the error.
	 */
	@Override
	public void onError(WebSocket websocket, Exception ex) {
		OpenemsSession session = this.websockets.get(websocket);
		if (session != null) {
			log.warn("OpenEMS connection error. Device [" + session.getData().getDevice().getName() + "]: ", ex);
		} else {
			log.warn("OpenEMS connection error: ", ex);
		}
	}

	/**
	 * Message event of websocket. Handles a new message. At this point the device is already authenticated.
	 */
	@Override
	public void onMessage(WebSocket websocket, String message) {
		MetadataDevice device = websockets.get(websocket).getData().getDevice();
		try {
			JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();

			// TODO Debugging
			if (!jMessage.has("timedata") && !jMessage.has("currentData")) {
				log.info("Received from " + device.getName() + ": " + jMessage.toString());
			}

			// Is this a reply?
			if (jMessage.has("id")) {
				forwardReplyToBrowser(jMessage);
			}

			/*
			 * New timestamped data
			 */
			if (jMessage.has("timedata")) {
				timedata(device, jMessage.get("timedata"));
			}
			// /*
			// * New currentdata data -> forward to browserWebsockets
			// */
			// if (jMessage.has("currentdata")) {
			// currentdata(websocket, jMessage.get("currentdata"));
			// }
			//
			// /*
			// * New log -> forward to browserWebsockets
			// */
			// if (jMessage.has("log")) {
			// log(websocket, jMessage.get("log"));
			// }
			//
			// /*
			// * New metadata
			// */
			// if (jMessage.has("metadata")) {
			// metadata(device, websocket, jMessage.get("metadata"));
			// }

			// Save data to Odoo

			device.writeObject();
		} catch (OpenemsException e) {
			log.error(device.getName() + ": " + e.getMessage());
		}
	}

	private void forwardReplyToBrowser(JsonObject jMessage) {
		try {
			// get browser websocket
			JsonArray jId = JsonUtils.getAsJsonArray(jMessage, "id");
			String token = JsonUtils.getAsString(jId.get(jId.size() - 1));
			Optional<WebSocket> browserWebsocketOpt = BrowserWebsocket.instance().getBrowserWebsocketByToken(token);
			if (!browserWebsocketOpt.isPresent()) {
				log.warn("Browser websocket is not connected.");
				return;
				// TODO: do an unsubscribe, otherwise OpenEMS keeps sending data unnecessarily:
				// [orker-13] [INFO ] [msWebsocketSingleton:149] Received from openems0:
				// {"id":["currentData","9s8pebngh23g07m9c7o3lrkh3s"],"currentData":{"meter0":{"ActivePower":-169,"ReactivePower":16},"ess0":{"ActivePower":0,"ReactivePower":0,"Soc":39},"meter1":{"ActivePower":-25,"ReactivePower":98}}}
				// [orker-13] [WARN ] [msWebsocketSingleton:199] Browser websocket is not connected.
			}
			WebSocket browserWebsocket = browserWebsocketOpt.get();

			// remove token from message id
			jId.remove(jId.size() - 1);
			jMessage.add("id", jId);

			// TODO debug log
			if (!jMessage.has("currentData")) {
				log.info("Forward to Browser: " + jMessage);
			}

			// send
			WebSocketUtils.send(browserWebsocket, jMessage);
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
	}

	private void timedata(MetadataDevice device, JsonElement jTimedataElement) {
		try {
			JsonObject jTimedata = JsonUtils.getAsJsonObject(jTimedataElement);
			// Write to InfluxDB
			try {
				Timedata.instance().write(device.getNameNumber(), jTimedata);
				log.debug(device.getName() + ": wrote " + jTimedata.entrySet().size() + " timestamps "
						+ StringUtils.toShortString(jTimedata, 120));
			} catch (Exception e) {
				log.error("No InfluxDB-connection: ", e);
			}
			// Write some data to Odoo
			// This is only to provide feedback for FENECON Service-Team that the device is online.
			device.setLastUpdate();
			jTimedata.entrySet().forEach(entry -> {
				try {
					JsonObject jChannels = JsonUtils.getAsJsonObject(entry.getValue());
					if (jChannels.has("ess0/Soc")) {
						int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
						device.setSoc(soc);
					}
					if (jChannels.has("system0/PrimaryIpAddress")) {
						String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
						device.setIpV4(ipv4);
					}
				} catch (OpenemsException e) {
					log.error(e.getMessage());
				}
			});
		} catch (OpenemsException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Forward currentdata to browserWebsockets
	 */
	private void currentdata(WebSocket websocket, JsonElement jCurrentdataElement) {
		// try {
		// JsonObject jCurrentdata = JsonUtils.getAsJsonObject(jCurrentdataElement);
		// JsonObject j = new JsonObject();
		// j.add("currentdata", jCurrentdata);
		// this.connectionManager.getFemsWebsocketDeviceNames(websocket).forEach(name -> {
		// j.addProperty("device", name);
		// this.connectionManager.getBrowserWebsockets(name).forEach(browserWebsocket -> {
		// // log.info("BrowserWS: " + browserWebsocket + ", " + browserWebsocket.isOpen());
		// log.info(name + ": forward currentdata to Browser: " + StringUtils.toShortString(j, 100));
		// WebSocketUtils.send(browserWebsocket, j);
		// });
		// });
		// } catch (OpenemsException e) {
		// log.error(e.getMessage());
		// }
	}

	private void log(WebSocket websocket, JsonElement jLogElement) {
		// try {
		// JsonObject jLog = JsonUtils.getAsJsonObject(jLogElement);
		// JsonObject j = new JsonObject();
		// j.add("log", jLog);
		// this.connectionManager.getFemsWebsocketDeviceNames(websocket).forEach(name -> {
		// j.addProperty("device", name);
		// this.connectionManager.getBrowserWebsockets(name).forEach(browserWebsocket -> {
		// log.info(name + ": forward log to Browser: " + StringUtils.toShortString(j, 100));
		// WebSocketUtils.send(browserWebsocket, j);
		// });
		// });
		// } catch (OpenemsException e) {
		// log.error(e.getMessage());
		// }
	}

	private void metadata(MetadataDevice device, WebSocket websocket, JsonElement jMetadataElement) {
		try {
			JsonObject jMetadata = JsonUtils.getAsJsonObject(jMetadataElement);
			if (jMetadata.has("config")) {
				JsonObject jConfig = JsonUtils.getAsJsonObject(jMetadata, "config");
				// log.info(getDeviceName(websocket) + ": got config " + StringUtils.toShortString(jConfig, 120));
				device.setOpenemsConfig(jConfig);
			}
		} catch (OpenemsException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onStart() {

	}

	/**
	 * Parses the apikey from websocket onOpen handshake
	 *
	 * @param handshake
	 * @return
	 */
	private Optional<String> parseApikeyFromHandshake(ClientHandshake handshake) {
		if (handshake.hasFieldValue("apikey")) {
			String apikey = handshake.getFieldValue("apikey");
			return Optional.ofNullable(apikey);
		}
		return Optional.empty();
	}

	/**
	 * Returns true if this device is currently connected
	 *
	 * @param name
	 * @return
	 */
	public boolean isOpenemsWebsocketConnected(String deviceName) {
		Optional<OpenemsSession> sessionOpt = this.sessionManager.getSessionByDeviceName(deviceName);
		if (!sessionOpt.isPresent()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the OpenemsWebsocket for the given device
	 *
	 * @param name
	 * @return
	 */
	public Optional<WebSocket> getOpenemsWebsocket(String deviceName) {
		Optional<OpenemsSession> sessionOpt = this.sessionManager.getSessionByDeviceName(deviceName);
		if (!sessionOpt.isPresent()) {
			return Optional.empty();
		}
		OpenemsSession session = sessionOpt.get();
		return Optional.ofNullable(this.websockets.inverse().get(session));
	}
}