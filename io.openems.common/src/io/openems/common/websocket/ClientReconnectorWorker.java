package io.openems.common.websocket;

import java.lang.reflect.Field;
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
			this.parent.logInfo(this.log, "Waiting till next WebSocket reconnect ["
					+ (this.minWaitSecondsBetweenRetries - waitedSeconds) + "s]");
			return;
		}
		this.lastTry = start;

		this.parent.logInfo(this.log, "Connecting WebSocket... [" + ws.getReadyState() + "]");

		if (ws.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
			// Copy of WebSocketClient#reconnectBlocking.
			// Do not 'reset' if WebSocket has never been connected before.
			this.parent.logInfo(this.log, "# Reset WebSocket Client...");
			resetWebSocketClient(ws, this.parent::createWsData, this.config.connectTimeoutSeconds());
			this.parent.logInfo(this.log, "# Reset WebSocket Client... done");
		}
		try {
			this.parent.logInfo(this.log, "# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]...");
			ws.connectBlocking(this.config.connectTimeoutSeconds(), TimeUnit.SECONDS);
			this.parent.logInfo(this.log, "# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]... done");

		} catch (IllegalStateException e) {
			// Catch "WebSocketClient objects are not reuseable" thrown by
			// WebSocketClient#connect(). Set WebSocketClient#connectReadThread to `null`.
			this.parent.logInfo(this.log, "# Reset WebSocket Client after Exception... " + e.getMessage());
			resetWebSocketClient(ws, this.parent::createWsData, this.config.connectTimeoutSeconds());
			this.parent.logInfo(this.log, "# Reset WebSocket Client after Exception... done");
		}

		var end = Instant.now();
		this.parent.logInfo(this.log,
				"Connected WebSocket successfully [" + Duration.between(start, end).toSeconds() + "s]");

		this.lastTry = end;
	}

	/**
	 * This method is a copy of {@link WebSocketClient} reset()-method, because the
	 * original one may block at the call of 'closeBlocking()' method. It also sets
	 * the new attachment from the attachment supplier.
	 * 
	 * <p>
	 * Waiting for https://github.com/TooTallNate/Java-WebSocket/pull/1251 to be
	 * merged.
	 * 
	 * @param <T>                   the type of the attachment
	 * @param ws                    the {@link WebSocketClient}
	 * @param wsData                {@link Function} to provide a the new attachment
	 * @param connectTimeoutSeconds the max wait time to close the websocket
	 * @throws Exception on error
	 */
	protected static <T extends WsData> void resetWebSocketClient(WebSocketClient ws, Function<WebSocket, T> wsData,
			int connectTimeoutSeconds) throws Exception {
		/*
		 * Get methods and fields via Reflection
		 */
		// WebSocketClient#writeThread
		Field writeThreadField = WebSocketClient.class.getDeclaredField("writeThread");
		writeThreadField.setAccessible(true);
		final var writeThread = (Thread) writeThreadField.get(ws);
		// WebSocketClient#connectReadThread
		Field connectReadThreadField = WebSocketClient.class.getDeclaredField("connectReadThread");
		connectReadThreadField.setAccessible(true);
		final var connectReadThread = (Thread) connectReadThreadField.get(ws);
		// WebSocketClient#draft
		Field draftField = WebSocketClient.class.getDeclaredField("draft");
		draftField.setAccessible(true);
		final var draft = (Draft) draftField.get(ws);
		// WebSocketClient#socket
		Field socketField = WebSocketClient.class.getDeclaredField("socket");
		socketField.setAccessible(true);
		final var socket = (Socket) socketField.get(ws);
		// WebSocketClient#connectLatch
		Field connectLatchField = WebSocketClient.class.getDeclaredField("connectLatch");
		connectLatchField.setAccessible(true);
		// WebSocketClient#closeLatch
		Field closeLatchField = WebSocketClient.class.getDeclaredField("closeLatch");
		closeLatchField.setAccessible(true);
		final var closeLatch = (CountDownLatch) closeLatchField.get(ws);
		// WebSocketClient#closeLatch
		Field engineField = WebSocketClient.class.getDeclaredField("engine");
		engineField.setAccessible(true);
		final var engine = (WebSocketImpl) engineField.get(ws);

		/*
		 * From here it's a copy of #reset()
		 */
		Thread current = Thread.currentThread();
		if (current == writeThread || current == connectReadThread) {
			throw new IllegalStateException(
					"You cannot initialize a reconnect out of the websocket thread. Use reconnect in another thread to ensure a successful cleanup.");
		}
		try {
			// closeBlocking(); -> to reflection
			ws.close();
			closeLatch.await(connectTimeoutSeconds, TimeUnit.SECONDS);
			// closeBlocking() END
			if (writeThread != null) {
				writeThread.interrupt();
				// writeThread = null; -> to reflection
				writeThreadField.set(ws, null);
			}
			if (connectReadThread != null) {
				connectReadThread.interrupt();
				// this.connectReadThread = null; -> to reflection
				connectReadThreadField.set(ws, null);
			}
			draft.reset();
			if (socket != null) {
				socket.close();
				// this.socket = null; -> to reflection
				socketField.set(ws, null);
			}
		} catch (Exception e) {
			ws.onError(e);
			engine.closeConnection(CloseFrame.ABNORMAL_CLOSE, e.getMessage());
			return;
		}
		// connectLatch = new CountDownLatch(1); -> to reflection
		connectLatchField.set(ws, new CountDownLatch(1));
		// closeLatch = new CountDownLatch(1); -> to reflection
		closeLatchField.set(ws, new CountDownLatch(1));
		// this.engine = new WebSocketImpl(this, this.draft); -> to reflection
		final var newEngine = new WebSocketImpl(ws, draft);
		final var newAttachment = wsData.apply(ws);
		newEngine.setAttachment(newAttachment);
		engineField.set(ws, newEngine);
	}

	@Override
	protected int getCycleTime() {
		return this.config.cycleTime();
	}

}
