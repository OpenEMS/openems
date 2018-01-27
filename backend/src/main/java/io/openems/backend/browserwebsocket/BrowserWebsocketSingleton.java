package io.openems.backend.browserwebsocket;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.management.Notification;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.browserwebsocket.session.BackendCurrentDataWorker;
import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.browserwebsocket.session.BrowserSessionManager;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.openemswebsocket.OpenemsWebsocketSingleton;
import io.openems.backend.openemswebsocket.session.OpenemsSession;
import io.openems.backend.timedata.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.Device;
import io.openems.common.types.DeviceImpl;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.WebSocketUtils;

/**
 * Handles connections from a browser.
 *
 * @author stefan.feilmeier
 *
 */
public class BrowserWebsocketSingleton
		extends AbstractWebsocketServer<BrowserSession, BrowserSessionData, BrowserSessionManager> {
	private final Logger log = LoggerFactory.getLogger(BrowserWebsocketSingleton.class);


