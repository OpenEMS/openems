package io.openems.common.websocket;

import java.lang.reflect.InvocationTargetException;
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
	private Instant lastTry = null;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws InterruptedException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var ws = this.parent.ws;
		if ((ws == null) || (ws.getReadyState() == ReadyState.OPEN)) {
			return;
		}

		var now = Instant.now();

		if (this.lastTry == null) {
			this.lastTry = now;
			return;
		}

		var waitedSeconds = Duration.between(this.lastTry, now).getSeconds();
		if (waitedSeconds < ClientReconnectorWorker.MIN_WAIT_SECONDS_BETWEEN_RETRIES) {
			this.parent.logInfo(this.log, "Waiting till next WebSocket reconnect ["
					+ (ClientReconnectorWorker.MIN_WAIT_SECONDS_BETWEEN_RETRIES - waitedSeconds) + "s]");
			return;
		}
		this.lastTry = now;

		this.parent.logInfo(this.log, "Reconnecting WebSocket...");

		// Copy of WebSocketClient#reconnectBlocking, but with timeout; need to use
		// reflection here because 'reset' is private.
		Method resetMethod = WebSocketClient.class.getDeclaredMethod("reset");
		resetMethod.setAccessible(true);
		resetMethod.invoke(ws);
		ws.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

		this.parent.logInfo(this.log,
				"Reconnected WebSocket successfully [" + Duration.between(now, Instant.now()).toSeconds() + "s]");
	}

	@Override
	protected int getCycleTime() {
		return 2 * 60 * 1000; /* 2 minutes */
	}

}
