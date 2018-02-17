package io.openems.common.websocket;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

public abstract class AbstractWebsocketServer extends WebSocketServer {
	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);

	protected abstract void _onMessage(WebSocket websocket, JsonObject jMessage);

	protected abstract void _onOpen(WebSocket websocket, ClientHandshake handshake);

	protected abstract void _onError(WebSocket websocket, Exception ex);

	protected abstract void _onClose(WebSocket websocket);

	public AbstractWebsocketServer(int port) {
		super(new InetSocketAddress(port), Lists.newArrayList(new Draft_6455()));
	}

	@Override
	public final void onStart() {
		// nothing to do
	}

	/**
	 * Open event of websocket.
	 */
	@Override
	public final void onOpen(WebSocket websocket, ClientHandshake handshake) {
		try {
			this._onOpen(websocket, handshake);
		} catch (Throwable e) {
			log.error("onOpen-Error [" + this.handshakeToJsonObject(handshake) + "]: ");
			e.printStackTrace();
		}
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public final void onMessage(WebSocket websocket, String message) {
		try {
			JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
			this._onMessage(websocket, jMessage);
		} catch (Throwable e) {
			log.error("onMessage-Error [" + message + "]: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Close event of websocket. Removes the websocket. Keeps the session. Calls
	 * _onClose()
	 */
	@Override
	public final void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		// try {
		// Optional<S> sessionOpt = Optional.ofNullable(this.websockets.get(websocket));
		// String sessionString;
		// if (sessionOpt.isPresent()) {
		// sessionString = sessionOpt.get().toString() + " ";
		// } else {
		// sessionString = "";
		// }
		// log.info(sessionString + "Websocket closed. Code [" + code + "] Reason [" +
		// reason + "]");
		// this.websockets.remove(websocket);
		this._onClose(websocket);
		// } catch (Throwable e) {
		// log.error("onClose-Error. Code [" + code + "] Reason [" + reason + "]: " +
		// e.getMessage());
		// }
	}

	/**
	 * Error event of websocket. Logs the error.
	 */
	@Override
	public final void onError(WebSocket websocket, Exception ex) {
		// S session = this.websockets.get(websocket);
		// String sessionString;
		// if (session == null) {
		// sessionString = "";
		// } else {
		// sessionString = session.toString();
		// }
		// log.warn(sessionString + " Websocket error: " + ex.getMessage());
		this._onError(websocket, ex);
	}

	/**
	 * Get cookie from handshake
	 *
	 * @param handshake
	 * @return cookie as JsonObject
	 */
	protected Optional<String> getSessionIdFromHandshake(ClientHandshake handshake) {
		JsonObject jCookie = new JsonObject();
		if (handshake.hasFieldValue("cookie")) {
			String cookieString = handshake.getFieldValue("cookie");
			for (String cookieVariable : cookieString.split("; ")) {
				String[] keyValue = cookieVariable.split("=");
				if (keyValue.length == 2) {
					jCookie.addProperty(keyValue[0], keyValue[1]);
				}
			}
		}
		return JsonUtils.getAsOptionalString(jCookie, "session_id");
	}

	/**
	 * Converts a Handshake to a JsonObject
	 * 
	 * @param handshake
	 * @return
	 */
	protected JsonObject handshakeToJsonObject(ClientHandshake handshake) {
		JsonObject j = new JsonObject();
		for (Iterator<String> iter = handshake.iterateHttpFields(); iter.hasNext();) {
			String field = iter.next();
			j.addProperty(field, handshake.getFieldValue(field));
		}
		return j;
	}

	/**
	 * Returns the Websocket for the given token
	 *
	 * @param name
	 * @return
	 */
	// public Optional<WebSocket> getWebsocketByToken(String token) {
	// Optional<S> sessionOpt = (Optional<S>)
	// this.sessionManager.getSessionByToken(token);
	// if (!sessionOpt.isPresent()) {
	// return Optional.empty();
	// }
	// S session = sessionOpt.get();
	// return Optional.ofNullable(this.websockets.inverse().get(session));
	// }
	//
	// protected void addWebsocket(WebSocket websocket, S session) {
	// synchronized (this.websockets) {
	// if (this.websockets.containsKey(websocket)) {
	// log.warn("Websocket [" + websocket + "] already existed. Replacing existing
	// one with session ["
	// + session + "]");
	// }
	// if (this.websockets.inverse().containsKey(session)) {
	// log.warn("Session [" + session + "] already existed. Replacing existing one
	// with websocket ["
	// + websocket + "]");
	// }
	// this.websockets.forcePut(websocket, session);
	// }
	// }
	//
	// protected void removeWebsocket(WebSocket websocket) {
	// synchronized (this.websockets) {
	// this.websockets.remove(websocket);
	// }
	// }
	//
	// protected Optional<S> getSessionFromWebsocket(WebSocket websocket) {
	// return Optional.ofNullable(this.websockets.get(websocket));
	// }
	//
	// protected Optional<WebSocket> getWebsocketFromSession(S session) {
	// return Optional.ofNullable(this.websockets.inverse().get(session));
	// }
	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public boolean send(WebSocket websocket, JsonObject j) {
		// System.out.println("SEND: websocket["+websocket+"]: " + j.toString());
		try {
			websocket.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			log.error("Websocket is not connected. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		}
	}
}
