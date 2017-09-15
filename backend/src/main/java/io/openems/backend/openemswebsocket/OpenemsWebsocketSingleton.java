package io.openems.backend.openemswebsocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections to OpenEMS-Devices.
 *
 * @author stefan.feilmeier
 *
 */
public class OpenemsWebsocketSingleton
		extends AbstractWebsocketServer<OpenemsSession, OpenemsSessionData, OpenemsSessionManager> {
	private final Logger log = LoggerFactory.getLogger(OpenemsWebsocketSingleton.class);

	protected OpenemsWebsocketSingleton(int port) throws Exception {
		super(port, new OpenemsSessionManager());
	}

	/**
	 * Open event of websocket. Parses the "apikey" and stores it in a new Session.
	 */
	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
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

			// create new session
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

			// announce browserWebsocket that this OpenEMS Edge was connected
			BrowserWebsocket.instance().openemsConnectionOpened(deviceName);

		} catch (OpenemsException e) {
			// send connection failed to OpenEMS
			JsonObject jReply = DefaultMessages.openemsConnectionFailedReply(e.getMessage());
			WebSocketUtils.send(websocket, jReply);
			// close websocket
			websocket.closeConnection(CloseFrame.REFUSE,
					"OpenEMS connection failed. Device [" + deviceName + "] Apikey [" + apikey + "]");
		}
	}

	/**
	 * Close event of websocket. Removes the session and the websocket.
	 */
	@Override
	public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		OpenemsSession session = this.websockets.get(websocket);
		sessionManager.removeSession(session);
		super.onClose(websocket, code, reason, remote);
	}

	/**
	 * Message event of websocket. Handles a new message. At this point the device is already authenticated.
	 */
	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		MetadataDevice device = websockets.get(websocket).getData().getDevice();

		// if (!jMessage.has("timedata") && !jMessage.has("currentData") && !jMessage.has("log")
		// && !jMessage.has("config")) {
		// log.info("Received from " + device.getName() + ": " + jMessage.toString());
		// }

		// Is this a reply?
		if (jMessage.has("id")) {
			forwardReplyToBrowser(websocket, device.getName(), jMessage);
		}

		/*
		 * New timestamped data
		 */
		if (jMessage.has("timedata")) {
			timedata(device, jMessage.get("timedata"));
		}

		// Save data to Odoo
		try {
			device.writeObject();
		} catch (OpenemsException e) {
			log.error(device.getName() + ": " + e.getMessage());
		}
	}

	private void forwardReplyToBrowser(WebSocket openemsWebsocket, String deviceName, JsonObject jMessage) {
		try {
			// get browser websocket
			JsonArray jId = JsonUtils.getAsJsonArray(jMessage, "id");
			String token = JsonUtils.getAsString(jId.get(jId.size() - 1));
			Optional<WebSocket> browserWebsocketOpt = BrowserWebsocket.instance().getWebsocketByToken(token);
			if (!browserWebsocketOpt.isPresent()) {
				log.warn("Browser websocket is not connected. Device [" + deviceName + "] Message [" + jMessage + "]");
				if (jMessage.has("currentData")) {
					// unsubscribe obsolete browser websocket
					WebSocketUtils.send(openemsWebsocket, DefaultMessages.currentDataSubscribe(jId, new JsonObject()));
				}
				if (jMessage.has("log")) {
					// unsubscribe obsolete browser websocket
					WebSocketUtils.send(openemsWebsocket, DefaultMessages.logUnsubscribe(jId));
				}
				return;
			}
			WebSocket browserWebsocket = browserWebsocketOpt.get();

			// remove token from message id
			jId.remove(jId.size() - 1);
			jMessage.add("id", jId);
			// always add device name
			jMessage.addProperty("device", deviceName);

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
				log.error("Unable to write Timedata: ", e);
			}
			// Write some data to Odoo
			// This is only to provide feedback for FENECON Service-Team that the device is online.
			device.setLastUpdate();
			device.setLastMessage();
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