package io.openems.common.websocket;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import io.openems.common.utils.StringUtils;

public abstract class AbstractWebsocketServer extends WebSocketServer {
	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);

	private static final int MAX_CONCURRENT_THREADS = 20;

	private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

	protected abstract AbstractOnMessage _onMessage(WebSocket websocket, String message);

	protected abstract AbstractOnOpen _onOpen(WebSocket websocket, ClientHandshake handshake);

	protected abstract AbstractOnError _onError(WebSocket websocket, Exception ex);

	protected abstract AbstractOnClose _onClose(WebSocket websocket, int code, String reason, boolean remote);

	public AbstractWebsocketServer(int port) {
		super(new InetSocketAddress(port), Lists.newArrayList(new Draft_6455()));
	}

	@Override
	public void stop(int arg0) throws InterruptedException {
		this.executor.shutdown();
		super.stop(arg0);
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
		this.executor.submit(this._onOpen(websocket, handshake));
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	@Override
	public final void onMessage(WebSocket websocket, String message) {
		this.executor.submit(this._onMessage(websocket, message));
	}

	/**
	 * Close event of websocket. Removes the websocket. Keeps the session. Calls
	 * _onClose()
	 */
	@Override
	public final void onClose(WebSocket websocket, int code, String reason, boolean remote) {
		this.executor.submit(this._onClose(websocket, code, reason, remote));
	}

	/**
	 * Error event of websocket. Logs the error.
	 */
	@Override
	public final void onError(WebSocket websocket, Exception ex) {
		this.executor.submit(this._onError(websocket, ex));
	}

	/**
	 * Send a message to a websocket.
	 *
	 * @param websocket the Websocket
	 * @param j         the JsonObject
	 * @return true if successful, otherwise false
	 */
	@Deprecated
	public boolean send(WebSocket websocket, JsonObject j) {
		try {
			websocket.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			log.error("Websocket is not connected. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		}
	}

	public void executorTryAgain(Runnable runnable) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.executor.submit(runnable);
	}
}
