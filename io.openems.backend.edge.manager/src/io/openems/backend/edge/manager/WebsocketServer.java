package io.openems.backend.edge.manager;

import java.util.Objects;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(EdgeManagerImpl parent, String name, int port, int poolSize) {
		super(name, port, poolSize);
		this.onOpen = new OnOpen(name, parent);
		this.onRequest = new OnRequest(//
				name, //
				parent);
		this.onNotification = new OnNotification(//
				name, //
				() -> parent.eventAdmin, //
				() -> parent.uiWebsocket, //
				() -> parent.timedataManager, //
				parent.subscriptionManager, //
				parent.metadata::getEdge, //
				parent.systemLogHandler::handleSystemLogNotification, //
				parent.scheduleHandler::handleScheduleNotification);
		this.onError = new OnError(name);
		this.onClose = new OnClose(//
				name, //
				parent.metadata::getEdge);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	/**
	 * Is the given Edge online?.
	 *
	 * @param edgeId the Edge-ID
	 * @return true if it is online.
	 */
	public boolean isOnline(String edgeId) {
		return this.getConnections().stream() //
				.map(ws -> (WsData) ws.getAttachment()) //
				.filter(Objects::nonNull) //
				.anyMatch(wsData -> wsData.containsEdgeId(edgeId));
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	public String debugLog() {
		return super.debugLog();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.warn(message);
	}

	@Override
	protected void logError(Logger log, String message) {
		log.error(message);
	}
}
