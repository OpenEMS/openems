package io.openems.common.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	private final static int MAX_WAIT_SECONDS = 120;
	private final static int MIN_WAIT_SECONDS = 10;

	private final static long MIN_WAIT_SEONDCS_BETWEEN_RETRIES = new Random().nextInt(MAX_WAIT_SECONDS)
			+ MIN_WAIT_SECONDS;

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private Instant lastTry = null;;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws InterruptedException {
		WebSocketClient ws = this.parent.ws;
		if (ws == null) {
			return;
		}

		if (ws.getReadyState() == ReadyState.OPEN) {
			return;
		}

		Instant now = Instant.now();

		if (this.lastTry == null) {
			this.lastTry = now;
			return;
		}

		long waitedSeconds = Duration.between(this.lastTry, now).getSeconds();
		if (waitedSeconds < MIN_WAIT_SEONDCS_BETWEEN_RETRIES) {
			this.parent.logInfo(this.log, "Waiting till next WebSocket reconnect ["
					+ (MIN_WAIT_SEONDCS_BETWEEN_RETRIES - waitedSeconds) + "s]");
			return;
		}
		this.lastTry = now;

		this.parent.logInfo(this.log, "Reconnecting WebSocket...");
		ws.reconnectBlocking();
	}

	@Override
	protected int getCycleTime() {
		return 2 * 60 * 1000; /* 2 minutes */
	}

}
