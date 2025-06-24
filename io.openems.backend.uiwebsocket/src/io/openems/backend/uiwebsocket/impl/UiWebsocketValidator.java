package io.openems.backend.uiwebsocket.impl;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketServer;

public class UiWebsocketValidator implements Runnable {

	// Set this value to e.g. 10 if you face crashing UiWebsocket-Server in
	// production
	private static final int FAULT_LIMIT = -1;

	private final Logger log = LoggerFactory.getLogger(UiWebsocketValidator.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private AbstractWebsocketServer<?> server = null;
	private Instant zeroSince = null;

	protected void start(AbstractWebsocketServer<?> server) {
		this.executor.scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
		this.server = server;
	}

	protected void stop() {
		this.server = null;
		shutdownAndAwaitTermination(this.executor, 0);
	}

	@Override
	public void run() {
		if (this.server == null) {
			return;
		}

		if (this.server.getConnections().size() > FAULT_LIMIT) {
			this.zeroSince = null;
			return;
		}

		var now = Instant.now();
		if (this.zeroSince == null) {
			this.zeroSince = now;
			return;
		}

		var zeroSinceSeconds = Duration.between(this.zeroSince, now).toSeconds();
		if (zeroSinceSeconds < 121) {
			this.log.warn("### UiWebsocketValidator: no UI connections! Will SYSTEM EXIT in " //
					+ (120 - zeroSinceSeconds) + "s");

		} else {
			this.log.error("### UiWebsocketValidator");
			this.log.error("### NO UI CONNECTIONS SINCE 120 SECONDS");
			this.log.error("### SYSTEM EXIT!");
			this.log.error("### UiWebsocketValidator");
			System.exit(1);
		}
	}
}
