package io.openems.common.websocket;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	public record Config(int connectTimeoutSeconds, int maxWaitSeconds, int minWaitSeconds, int cycleTime) {

	}

	public static final ClientReconnectorWorker.Config DEFAULT_CONFIG = new Config(100, 100, 10,
			2 * 60 * 1000 /* 2 minutes */);

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private final Config config;
	private final long minWaitSecondsBetweenRetries;
	private Instant lastTry = Instant.MIN;
	private String debugLog = null;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent, Config config) {
		this.parent = parent;
		this.config = config;
		this.minWaitSecondsBetweenRetries = new Random().nextInt(config.maxWaitSeconds()) + config.minWaitSeconds();
	}

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this(parent, ClientReconnectorWorker.DEFAULT_CONFIG);
	}

	@Override
	protected void forever() throws Exception {
		var ws = this.parent.ws;
		if (ws == null || ws.getReadyState() == ReadyState.OPEN) {
			return;
		}

		var start = Instant.now();
		var waitedSeconds = Duration.between(this.lastTry, start).getSeconds();
		if (waitedSeconds < this.minWaitSecondsBetweenRetries) {
			this.debugLog = "Waiting till next WebSocket reconnect ["
					+ (this.minWaitSecondsBetweenRetries - waitedSeconds) + "s]";
			return;
		}
		this.lastTry = start;

		this.debugLog = "Connecting WebSocket... [" + ws.getReadyState() + "]";

		if (ws.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
			// Copy of WebSocketClient#reconnectBlocking.
			// Do not 'reset' if WebSocket has never been connected before.
			resetWebSocketClient(ws, this.parent::createWsData);
		}

		var success = false;
		try {
			this.parent.logInfo(this.log, "# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]...");
			success = ws.connectBlocking(this.config.connectTimeoutSeconds(), TimeUnit.SECONDS);
			this.parent.logInfo(this.log, "# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]... done");

		} catch (IllegalStateException e) {
			// Catch "WebSocketClient objects are not reuseable" thrown by
			// WebSocketClient#connect(). Set WebSocketClient#connectReadThread to `null`.
			this.parent.logInfo(this.log, "# Reset WebSocket Client after Exception... " + e.getMessage());
			resetWebSocketClient(ws, this.parent::createWsData);
			this.parent.logInfo(this.log, "# Reset WebSocket Client after Exception... done");
		}

		var end = Instant.now();
		if (success) {
			this.debugLog = null;
			this.parent.logInfo(this.log, "Connected successfully [" + Duration.between(start, end).toSeconds() + "s]");
		} else {
			this.debugLog = "Connection failed";
			this.log.info("Connection failed");
		}

		this.lastTry = end;
	}

	/**
	 * This method uses the {@link WebSocketClient} reset()-method through reflection,
	 * because it is private. It also sets the new attachment from the attachment supplier.
	 * 
	 * @param <T>                   the type of the attachment
	 * @param ws                    the {@link WebSocketClient}
	 * @param wsData                {@link Function} to provide a the new attachment
	 * @throws Exception on error
	 */
	protected static <T extends WsData> void resetWebSocketClient(WebSocketClient ws, Function<WebSocket, T> wsData) throws Exception {
		// get the private WebSocketClient#reset method via Reflection
		Method reset = WebSocketClient.class.getDeclaredMethod("reset");
		reset.setAccessible(true);
		reset.invoke(ws);

		final var newAttachment = wsData.apply(ws);
		ws.setAttachment(newAttachment);
	}

	@Override
	protected int getCycleTime() {
		return this.config.cycleTime();
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		var message = this.debugLog;
		return message == null //
				? "" //
				: message;
	}

}
