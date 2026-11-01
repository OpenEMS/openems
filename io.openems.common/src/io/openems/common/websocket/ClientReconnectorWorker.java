package io.openems.common.websocket;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

	private static final int THREAD_CYCLE_MILLIS = 1_000;

	private final Logger log = LoggerFactory.getLogger(ClientReconnectorWorker.class);
	private final AbstractWebsocketClient<?> parent;
	private final Config config;
	private final long minWaitSecondsBetweenRetries;
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
		if (this.parent.ws.isOpen()) {
			this.debugLog = "ALIVE";
			this.parent.checkConnectionLost();
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

		this.logAndSetDebugInfo("Connecting WebSocket...");
		this.logAndSetDebugInfo("# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]...");
		var success = this.parent.connect(this.config.connectTimeoutSeconds());
		this.logAndSetDebugInfo("# Connect Blocking [" + this.config.connectTimeoutSeconds() + "]... done");

		if (success) {
			this.callEvent(WebsocketReconnectorEvent.CONNECTED);
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

	@Override
	protected int getCycleTime() {
		return THREAD_CYCLE_MILLIS;
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		return this.debugLog;
	}

	/**
	 * Handles a failed WebSocket handshake by updating the debug log and emitting
	 * the corresponding reconnect event.
	 *
	 * @param reason a short description of why the handshake failed
	 */
	public void notifyHandshakeFailed(String reason) {
		this.logAndSetDebugInfo(reason);
		this.callEvent(WebsocketReconnectorEvent.HANDSHAKE_FAILED);
	}

	public enum WebsocketReconnectorEvent {
		RESET_WEBSOCKET_CLIENT, CLOSE_FAILED, CONNECTED, HANDSHAKE_FAILED
	}

}
