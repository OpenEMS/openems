package io.openems.edge.bridge.http;

import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.common.event.EdgeEventConstants;

@Component(//
		scope = ServiceScope.PROTOTYPE //
)
public class BridgeHttpImpl implements BridgeHttp {

	private static class EndpointCountdown {
		public int cnt;
		public final Endpoint endpoint;
		public boolean isRunning = false;

		public EndpointCountdown(Endpoint endpoint) {
			super();
			this.cnt = endpoint.cycle();
			this.endpoint = endpoint;
		}

		public EndpointCountdown reset() {
			this.cnt = this.endpoint.cycle();
			return this;
		}

	}

	private final Logger log = LoggerFactory.getLogger(BridgeHttpImpl.class);

	@Reference
	private CycleSubscriber cycleSubscriber;

	@Reference
	private UrlFetcher urlFetcher;

	// TODO change to java 21 virtual threads
	// TODO: Single pool for every http worker & avoid same endpoint in that pool
	private final ExecutorService pool = Executors.newCachedThreadPool();

	private final PriorityQueue<EndpointCountdown> endpoints = new PriorityQueue<>((e1, e2) -> e1.cnt - e2.cnt);

	/*
	 * Default timeout values in ms
	 */
	private int connectTimeout = 5000;
	private int readTimeout = 5000;

	@Activate
	protected void activate() {
		this.cycleSubscriber.subscribe(this::handleEvent);
	}

	@Deactivate
	protected void deactivate() {
		this.cycleSubscriber.unsubscribe(this::handleEvent);
		this.endpoints.clear();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.pool, 0);
	}

	@Override
	public void subscribe(Endpoint endpoint) {
		this.endpoints.add(new EndpointCountdown(endpoint));
	}

	@Override
	public CompletableFuture<String> request(String url) {
		final var future = new CompletableFuture<String>();
		this.pool.execute(this.urlFetcher.createTask(url, this.connectTimeout, this.readTimeout, future));
		return future;
	}

	@Override
	public void setTimeout(int connectTimeout, int readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	private void handleEvent(Event event) {
		switch (event.getTopic()) {
		// TODO: Execute before TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, like modbus bridge
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {

			this.endpoints.forEach(t -> t.cnt--);

			if (this.endpoints.isEmpty()) {
				return;
			}
			while (this.endpoints.peek().cnt == 0) {
				final var item = this.endpoints.poll();
				synchronized (item) {
					if (item.isRunning) {
						this.log.info("Process for " + item.endpoint + " is still running.");
						this.endpoints.add(item.reset());
						return;
					}

					item.isRunning = true;
				}
				this.pool.execute(this.createTask(item));

				this.endpoints.add(item.reset());
			}
		}
		}
	}

	private Runnable createTask(EndpointCountdown endpointItem) {
		final var future = new CompletableFuture<String>();
		future.whenComplete((t, e) -> {
			try {
				if (e != null) {
					endpointItem.endpoint.onError().accept(e);
					return;
				}
				endpointItem.endpoint.result().accept(t);
			} finally {
				synchronized (endpointItem) {
					endpointItem.isRunning = false;
				}
			}
		});
		return this.urlFetcher.createTask(endpointItem.endpoint.url(), this.connectTimeout, this.readTimeout, future);
	}
}
