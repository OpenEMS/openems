package io.openems.common.websocket;

import java.lang.reflect.Field;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.utils.FunctionUtils;
import io.openems.common.worker.AbstractWorker;

public class ClientReconnectorWorker extends AbstractWorker {

	public record Config(int connectTimeoutSeconds, int maxWaitSeconds, int minWaitSeconds,
			Consumer<WebsocketReconnectorEvent> onEvent) {

		/**
		 * Copies configuration and sets event handler.
		 *
		 * @param onEvent Event handler to set
		 * @return New config instance
		 */
		public Config withEventHandler(Consumer<WebsocketReconnectorEvent> onEvent) {
			return new Config(this.connectTimeoutSeconds, this.maxWaitSeconds, this.minWaitSeconds, onEvent);
		}
	}

	public static final ClientReconnectorWorker.Config DEFAULT_CONFIG = new Config(100, 100, 10,
			FunctionUtils::doNothing);

	private static final long CLOSE_TIMEOUT_MILLIS = 60_000L;
	private static final int THREAD_CYCLE_MILLIS = 1_000;

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private final Config config;
	private final long minWaitSecondsBetweenRetries;
	private final List<String> additionalLogInfos = new CopyOnWriteArrayList<>();
	private Stopwatch timeSinceLastTry;
	private String debugLog = null;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent, Config config) {
		this.parent = parent;
		this.config = config;
		this.minWaitSecondsBetweenRetries = ThreadLocalRandom.current() //
				.nextInt(config.minWaitSeconds, config.maxWaitSeconds + 1);
	}

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent) {
		this(parent, ClientReconnectorWorker.DEFAULT_CONFIG);
	}

	@Override
	protected void forever() throws Exception {
		var ws = this.parent.ws;
		if (ws == null || ws.getReadyState() == ReadyState.OPEN) {
			this.debugLog = "ALIVE";
			return;
		}

		if (this.timeSinceLastTry == null) {
			this.timeSinceLastTry = Stopwatch.createStarted();
		} else if (this.timeSinceLastTry.elapsed(TimeUnit.SECONDS) < this.minWaitSecondsBetweenRetries) {
			this.debugLog = "Waiting till next WebSocket reconnect ["
					+ (this.minWaitSecondsBetweenRetries - this.timeSinceLastTry.elapsed(TimeUnit.SECONDS)) + "s]";
			return;
		}

		this.timeSinceLastTry.reset();
		this.timeSinceLastTry.start();

		this.logAndSetDebugInfo("Connecting WebSocket... [" + ws.getReadyState() + "]");

		if (ws.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
			// Copy of WebSocketClient#reconnectBlocking.
			// Do not 'reset' if WebSocket has never been connected before.
			this.resetWebSocketClient(ws, this.parent::createWsData, this.config.connectTimeoutSeconds());
		}

		var success = false;
		try {
			this.logAndSetDebugInfo("# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]...");
			success = ws.connectBlocking(this.config.connectTimeoutSeconds(), TimeUnit.SECONDS);
			this.callEvent(WebsocketReconnectorEvent.CONNECTED);
			this.logAndSetDebugInfo("# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]... done");

		} catch (IllegalStateException e) {
			// Catch "WebSocketClient objects are not reuseable" thrown by
			// WebSocketClient#connect(). Set WebSocketClient#connectReadThread to `null`.
			this.logAndSetDebugInfo("# Reset WebSocket Client after Exception... " + e.getMessage());
			this.resetWebSocketClient(ws, this.parent::createWsData, this.config.connectTimeoutSeconds());
			this.logAndSetDebugInfo("# Reset WebSocket Client after Exception... done");
		}

		if (success) {
			this.logAndSetDebugInfo(
					"Connected successfully [" + this.timeSinceLastTry.elapsed(TimeUnit.SECONDS) + "s]");
		} else {
			this.logAndSetDebugInfo("Connection failed");
		}
	}

	private void logAndSetDebugInfo(String message) {
		this.debugLog = message;
		this.parent.logInfo(this.log, message);
	}

	private void callEvent(WebsocketReconnectorEvent event) {
		try {
			this.config.onEvent().accept(event);
		} catch (RuntimeException ex) {
			this.log.warn("Failed to handle websocket reconnect event '{}'", event.getClass().getSimpleName(), ex);
		}
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
	protected <T extends WsData> void resetWebSocketClient(WebSocketClient ws, Function<WebSocket, T> wsData,
			int connectTimeoutSeconds) throws Exception {
		this.callEvent(WebsocketReconnectorEvent.RESET_WEBSOCKET_CLIENT);

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

		var timer = Stopwatch.createStarted();

		/*
		 * From here it's a copy of #reset()
		 */
		Thread current = Thread.currentThread();
		if (current == writeThread || current == connectReadThread) {
			throw new IllegalStateException(
					"You cannot initialize a reconnect out of the websocket thread. Use reconnect in another thread to ensure a successful cleanup.");
		}
		var closeSuccess = FunctionUtils.runWithTimeout(this.thread.getName() + "::Close", CLOSE_TIMEOUT_MILLIS, () -> {
			try {
				// This socket null check ensures we can reconnect a socket that failed to
				// connect. It's an uncommon edge case, but we want to make sure we support it
				if (engine.getReadyState() == ReadyState.NOT_YET_CONNECTED && socket != null) {
					// Closing the socket when we have not connected prevents the writeThread from
					// hanging on a write indefinitely during connection teardown
					socket.close(); // This can deadlock
				}

				// closeBlocking(); -> to reflection
				ws.close();
				closeLatch.await(10, TimeUnit.SECONDS);
				// closeBlocking() END
				if (writeThread != null) {
					writeThread.interrupt();
					writeThread.join();
				}
				if (connectReadThread != null) {
					connectReadThread.interrupt();
					connectReadThread.join();
				}
				draft.reset();
				if (socket != null) {
					socket.close(); // This can deadlock
				}
			} catch (InterruptedException ie) {
				ws.onError(ie);
				// We are not calling closeConnection() because that would deadlock as well.
			} catch (Exception e) {
				ws.onError(e);
				engine.closeConnection(CloseFrame.ABNORMAL_CLOSE, e.getMessage());
			}
		});
		switch (closeSuccess) {
		case FunctionUtils.RunWithTimeoutResult.Success() -> {
			this.log.info("Closed websocket connection after {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
		}
		case FunctionUtils.RunWithTimeoutResult.TimeoutReached(var stacktrace) -> {
			this.log.error(
					"Failed to close socket. Timeout reached. Connection is still open and we continue with a new connection. {}",
					stacktrace);
			this.additionalLogInfos.add("{CLOSE_FAILED: " + stacktrace + "}");
			this.callEvent(WebsocketReconnectorEvent.CLOSE_FAILED);
		}
		}

		writeThreadField.set(ws, null);
		connectReadThreadField.set(ws, null);
		socketField.set(ws, null);

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
		return THREAD_CYCLE_MILLIS;
	}

	@Override
	protected int getMinSleepTime() {
		return 0;
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		return Stream.concat(Stream.of(this.debugLog), this.additionalLogInfos.stream()) //
				.filter(Objects::nonNull) //
				.collect(Collectors.joining(", "));
	}

	public enum WebsocketReconnectorEvent {
		RESET_WEBSOCKET_CLIENT, CLOSE_FAILED, CONNECTED
	}

}
