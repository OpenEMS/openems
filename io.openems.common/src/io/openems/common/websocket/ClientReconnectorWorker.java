package io.openems.common.websocket;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	private static final int CONNECT_TIMEOUT_SECONDS = 100;
	private static final int MAX_WAIT_SECONDS = 100;
	private static final int MIN_WAIT_SECONDS = 10;

	private static final long MIN_WAIT_SECONDS_BETWEEN_RETRIES = new Random()
			.nextInt(ClientReconnectorWorker.MAX_WAIT_SECONDS) + ClientReconnectorWorker.MIN_WAIT_SECONDS;

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private Instant lastTry = Instant.MIN;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws Exception {
		var ws = this.parent.ws;
		if (ws == null || ws.getReadyState() == ReadyState.OPEN) {
			return;
		}

		var start = Instant.now();
		var waitedSeconds = Duration.between(this.lastTry, start).getSeconds();
		if (waitedSeconds < ClientReconnectorWorker.MIN_WAIT_SECONDS_BETWEEN_RETRIES) {
			this.parent.logInfo(this.log, "Waiting till next WebSocket reconnect ["
					+ (ClientReconnectorWorker.MIN_WAIT_SECONDS_BETWEEN_RETRIES - waitedSeconds) + "s]");
			return;
		}
		this.lastTry = start;

		this.parent.logInfo(this.log, "Connecting WebSocket...");

		if (ws.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
			// Copy of WebSocketClient#reconnectBlocking.
			// Do not 'reset' if WebSocket has never been connected before.
			resetWebSocketClient(ws);
		}
		try {
			ws.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (IllegalStateException e) {
			// Catch "WebSocketClient objects are not reuseable" thrown by
			// WebSocketClient#connect(). Set WebSocketClient#connectReadThread to `null`.
			resetWebSocketClient(ws);
		}

		var end = Instant.now();
		this.parent.logInfo(this.log,
				"Connected WebSocket successfully [" + Duration.between(start, end).toSeconds() + "s]");

		this.lastTry = end;
	}

	/**
	 * This method calls {@link WebSocketClient} reset()-method via reflection
	 * because it is private.
	 * 
	 * <p>
	 * Waiting for https://github.com/TooTallNate/Java-WebSocket/pull/1251 to be
	 * merged.
	 * 
	 * @param ws the {@link WebSocketClient}
	 * @throws Exception on error
	 */
	private static void resetWebSocketClient(WebSocketClient ws) throws Exception {
		Method resetMethod = WebSocketClient.class.getDeclaredMethod("reset");
		resetMethod.setAccessible(true);
		resetMethod.invoke(ws);
	}

	@Override
	protected int getCycleTime() {
		return 2 * 60 * 1000; /* 2 minutes */
	}

}
