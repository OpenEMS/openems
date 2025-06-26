package io.openems.common.websocket;

import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;

public class OnCloseHandler implements Runnable {

	private final WebSocket ws;
	private final int code;
	private final String reason;
	private final boolean remote;
	private final OnClose onClose;
	private final BiConsumer<Throwable, String> handleInternalError;

	public OnCloseHandler(//
			WebSocket ws, int code, String reason, boolean remote, OnClose onClose, //
			BiConsumer<Throwable, String> handleInternalError) {
		this.ws = ws;
		this.code = code;
		this.reason = reason;
		this.remote = remote;
		this.onClose = onClose;
		this.handleInternalError = handleInternalError;
	}

	@Override
	public final void run() {
		try {
			this.onClose.accept(this.ws, this.code, this.reason, this.remote);

			// dispose WsData
			WsData wsData = this.ws.getAttachment();
			wsData.dispose();

		} catch (Throwable t) {
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}

}
