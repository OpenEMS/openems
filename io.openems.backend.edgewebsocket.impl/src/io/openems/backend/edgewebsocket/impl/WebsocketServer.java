package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.OnInternalError;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketServer.class);

	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final OnInternalError onInternalError;

	public WebsocketServer(EdgeWebsocketImpl parent, String name, int port) {
		super(name, port);
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError();
		this.onClose = new OnClose(parent);
		this.onInternalError = (ex) -> {
			log.info("OnInternalError: " + ex.getMessage());
			ex.printStackTrace();
		};
	}

//	public boolean isOnline(int edgeId) {
//		return this.websocketsMap.containsKey(edgeId);
//	}
//
//	protected String[] getEdgeNames(int[] edgeIds) {
//		String[] edgeNames = new String[edgeIds.length];
//		for (int i = 0; i < edgeIds.length; i++) {
//			Optional<Edge> edgeOpt = this.parent.metadataService.getEdgeOpt(edgeIds[i]);
//			if (edgeOpt.isPresent()) {
//				edgeNames[i] = edgeOpt.get().getName();
//			} else {
//				edgeNames[i] = "ID:" + edgeIds[i];
//			}
//		}
//		return edgeNames;
//	}

//	public void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException {
//		WebSocket websocket = this.websocketsMap.get(edgeId);
//		if (websocket != null) {
//			WebSocketUtils.send(websocket, jMessage);
//		}
//	}
//
	@Override
	protected WsData createWsData() {
		WsData wsData = new WsData();
		return wsData;
	}

	public boolean isOnline(String edgeId) {
		final Optional<String> edgeIdOpt = Optional.of(edgeId);
		return this.getConnections().parallelStream().anyMatch(
				ws -> ws.getAttachment() != null && ((WsData) ws.getAttachment()).getEdgeId().equals(edgeIdOpt));
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return this.onInternalError;
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
		return onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}
}
