package io.openems.common.websocket;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	private final static int MAX_WAIT_TIME_SECONDS = 120;
	private final static int MIN_WAIT_TIME_SECONDS = 10;

	private final static Duration MIN_WAIT_TIME_BETWEEN_RETRIES = Duration
			.ofSeconds(new Random().nextInt(MAX_WAIT_TIME_SECONDS) + MIN_WAIT_TIME_SECONDS);

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private LocalDateTime lastTry = LocalDateTime.MIN;

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

		Duration notWaitedEnough = Duration.between(LocalDateTime.now().minus(MIN_WAIT_TIME_BETWEEN_RETRIES),
				this.lastTry);
		if (!notWaitedEnough.isNegative()) {
			this.parent.logInfo(this.log,
					"Waiting till next WebSocket reconnect [" + notWaitedEnough.getSeconds() + "s]");
			return;
		}
		this.lastTry = LocalDateTime.now();

		this.parent.logInfo(this.log, "Reconnecting WebSocket...");
		ws.reconnectBlocking();
	}

	@Override
	protected int getCycleTime() {
		return 2 * 60 * 1000; /* 2 minutes */
	}

}
