package io.openems.common.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	private static final int MAX_WAIT_SECONDS = 120;
	private static final int MIN_WAIT_SECONDS = 10;

	private static final long MIN_WAIT_SEONDCS_BETWEEN_RETRIES = new Random()
			.nextInt(ClientReconnectorWorker.MAX_WAIT_SECONDS) + ClientReconnectorWorker.MIN_WAIT_SECONDS;

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private Instant lastTry = null;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws InterruptedException {
		var ws = this.parent.ws;
		if (ws == null) {
			return;
		}

		if (ws.getReadyState() == ReadyState.OPEN) {
			return;
		}

		var now = Instant.now();

		if (this.lastTry == null) {
			this.lastTry = now;
			return;
		}

		var waitedSeconds = Duration.between(this.lastTry, now).getSeconds();
		if (waitedSeconds < ClientReconnectorWorker.MIN_WAIT_SEONDCS_BETWEEN_RETRIES) {
			this.parent.logInfo(this.log, "Waiting till next WebSocket reconnect ["
					+ (ClientReconnectorWorker.MIN_WAIT_SEONDCS_BETWEEN_RETRIES - waitedSeconds) + "s]");
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
