package io.openems.common.websocket;

import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for WebSocket OnOpen event.
 */
public final class OnOpenHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(OnOpenHandler.class);
	private final WebSocket ws;
	private final Handshakedata handshake;
	private final OnOpen onOpen;
	private final BiConsumer<Logger, String> logWarn;
	private final BiConsumer<Throwable, String> handleInternalError;

	public OnOpenHandler(//
			WebSocket ws, Handshakedata handshake, OnOpen onOpen, //
			BiConsumer<Logger, String> logWarn, //
			BiConsumer<Throwable, String> handleInternalError) {
		this.ws = ws;
		this.handshake = handshake;
		this.onOpen = onOpen;
		this.logWarn = logWarn;
		this.handleInternalError = handleInternalError;
	}

	@Override
	public final void run() {
		try {
			var error = this.onOpen.apply(this.ws, this.handshake);
			if (error != null) {
				this.logWarn.accept(this.log, "Error during OnOpen of " + generateWsDataString(this.ws));
			}

		} catch (WebsocketNotConnectedException e) {
			this.logWarn.accept(this.log,
					"Websocket was closed before it has been fully opened: " + generateWsDataString(this.ws));

		} catch (Throwable t) {
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}
}