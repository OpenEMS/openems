package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.UserDevicesInfo;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class UiWebsocketServer extends AbstractWebsocketServer {
	
	private final Logger log = LoggerFactory.getLogger(UiWebsocketServer.class);
	private final UiWebsocket parent;

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String error = "";
		
		// login using session_id from the cookie
		Optional<String> sessionIdOpt = getSessionIdFromHandshake(handshake);
		if(!sessionIdOpt.isPresent()) {
			error = "Session-ID is missing in handshake";
		} else {
			UserDevicesInfo info;
			try {
				info = this.parent.getMetadataService().getInfoWithSession(sessionIdOpt.get());
				System.out.println(info.getUserName());
			} catch (OpenemsException e) {
				error = e.getMessage();
			}
		}
		System.out.println(error);

//		// check if the session is now valid and send reply to browser
//		BrowserSessionData data = session.getData();
//		if (error.isEmpty()) {
//			// add isOnline information
//			OpenemsWebsocketSingleton openemsWebsocket = OpenemsWebsocket.instance();
//			for (DeviceImpl device : data.getDevices()) {
//				device.setOnline(openemsWebsocket.isOpenemsWebsocketConnected(device.getName()));
//			}
//
//			// send connection successful to browser
//			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply(session.getToken(), Optional.empty(),
//					data.getDevices());
//			// TODO write user name to log output
//			WebSocketUtils.send(websocket, jReply);
//
//			// add websocket to local cache
//			this.addWebsocket(websocket, session);
//
//			log.info("User [" + data.getUserName() + "] connected with Session [" + data.getOdooSessionId().orElse("")
//					+ "].");
//
//		} else {
//			// send connection failed to browser
//			JsonObject jReply = DefaultMessages.browserConnectionFailedReply();
//			WebSocketUtils.send(websocket, jReply);
//			log.warn("User [" + data.getUserName() + "] connection failed. Session ["
//					+ data.getOdooSessionId().orElse("") + "] Error [" + error + "].");
//
//			websocket.closeConnection(CloseFrame.REFUSE, error);
//		}
	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		log.info("UiWebsocketServer: On Message");
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.info("UiWebsocketServer: On Error");
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		log.info("UiWebsocketServer: On Close");
	}
}
