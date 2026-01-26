package io.openems.backend.edge.application;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.edge.jsonrpc.ConnectedEdges;
import io.openems.backend.edge.client.WebsocketClient;
import io.openems.backend.edge.server.WebsocketServer;
import io.openems.backend.edge.server.WsData;

public class CyclicTask implements Runnable {

	private final Logger log = LoggerFactory.getLogger(CyclicTask.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private final WebsocketClient client;
	private final Supplier<WebsocketServer> server;
	private final AtomicBoolean connectedEdgesChangedSinceLastRun = new AtomicBoolean(false);

	/**
	 * Builds and starts the {@link CyclicTask} via a given
	 * {@link ScheduledExecutorService}.
	 *
	 * @param client the {@link WebsocketClient}
	 * @param server the {@link WebsocketServer}
	 * @return a new {@link CyclicTask}
	 */
	public static CyclicTask from(WebsocketClient client, Supplier<WebsocketServer> server) {
		var task = new CyclicTask(client, server);
		task.executor.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
		return task;
	}

	protected void deactivate() {
		shutdownAndAwaitTermination(this.executor, 0);
	}

	public CyclicTask(WebsocketClient client, Supplier<WebsocketServer> server) {
		this.client = client;
		this.server = server;
	}

	@Override
	public void run() {
		try {
			this.printDebugLog();
			this.sendConnectedEdgesNotification();
		} catch (Exception e) {
			this.log.error(e.getMessage());
			this.log.debug("Stacktrace:", e);
		}
	}

	/**
	 * Announce Connected-Edges changed.
	 */
	public void connectedEdgesChanged() {
		this.connectedEdgesChangedSinceLastRun.set(true);
	}

	private void printDebugLog() {
		this.log.info(this.client.debugLog());
		var server = this.server.get();
		if (server != null) {
			this.log.info(server.debugLog());
		}
	}

	private void sendConnectedEdgesNotification() {
		var server = this.server.get();
		if (server == null || !this.connectedEdgesChangedSinceLastRun.getAndSet(false)) {
			return;
		}

		// connected Edges to Server changed since last run of CyclicTask
		// TODO Performance test. How long does this take? Better record/send only diff?
		var edgeIds = server.getConnections().stream() //
				.map(ws -> (WsData) ws.getAttachment()) //
				.map(wsData -> wsData.getEdgeId()) //
				.filter(Objects::nonNull) //
				.collect(toSet());
		var metrics = server.debugMetrics();
		this.client.sendMessage(new ConnectedEdges.Notification(edgeIds, metrics));
	}

}
