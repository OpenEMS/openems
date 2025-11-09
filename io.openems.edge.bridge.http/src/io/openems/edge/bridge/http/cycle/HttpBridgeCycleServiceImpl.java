package io.openems.edge.bridge.http.cycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.edge.common.event.EdgeEventConstants;

public class HttpBridgeCycleServiceImpl implements HttpBridgeService, HttpBridgeCycleService {

	public static class CycleEndpointCountdown {
		private volatile int cycleCount;
		private final CycleEndpoint cycleEndpoint;
		private volatile boolean running = false;

		public CycleEndpointCountdown(CycleEndpoint endpoint) {
			this.cycleCount = endpoint.cycle();
			this.cycleEndpoint = endpoint;
		}

		public CycleEndpoint getCycleEndpoint() {
			return this.cycleEndpoint;
		}

		/**
		 * Resets the current cycle count to the initial cycle count.
		 *
		 * @return this
		 */
		public CycleEndpointCountdown reset() {
			return this.resetTo(this.cycleEndpoint.cycle());
		}

		/**
		 * Resets the current cycle count to the given cycle count.
		 *
		 * @param cycleCount the cycleCount to reset the current cycleCount to
		 * @return this
		 */
		public CycleEndpointCountdown resetTo(int cycleCount) {
			this.cycleCount = cycleCount;
			return this;
		}

		public int getCycleCount() {
			return this.cycleCount;
		}

		/**
		 * Decreases the current cycle count by one.
		 */
		public void decreaseCycleCount() {
			this.cycleCount--;
		}

		public boolean isRunning() {
			return this.running;
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

	}

	private final Logger log = LoggerFactory.getLogger(HttpBridgeCycleServiceImpl.class);

	private final BridgeHttp bridgeHttp;
	private final CycleSubscriber cycleSubscriber;
	private final EndpointFetcher urlFetcher;
	private final BridgeHttpExecutor pool;

	private final PriorityBlockingQueue<CycleEndpointCountdown> cycleEndpoints = new PriorityBlockingQueue<>(11,
			(e1, e2) -> e1.getCycleCount() - e2.getCycleCount());

	public HttpBridgeCycleServiceImpl(BridgeHttp bridgeHttp, CycleSubscriber cycleSubscriber, EndpointFetcher urlFetcher,
			BridgeHttpExecutor pool) {
		this.bridgeHttp = bridgeHttp;
		this.cycleSubscriber = cycleSubscriber;
		this.cycleSubscriber.subscribe(this::handleEvent);
		this.urlFetcher = urlFetcher;
		this.pool = pool;
	}

	@Override
	public void close() throws Exception {
		this.cycleSubscriber.unsubscribe(this::handleEvent);
		this.cycleEndpoints.clear();
	}

	@Override
	public CycleEndpoint subscribeCycle(CycleEndpoint endpoint) {
		Objects.requireNonNull(endpoint, "CycleEndpoint is not allowed to be null!");

		if (!this.cycleEndpoints.offer(new CycleEndpointCountdown(endpoint))) {
			this.log.warn("Unable to add " + endpoint + "!");
			return null;
		}
		return endpoint;
	}

	@Override
	public Collection<CycleEndpoint> removeCycleEndpointIf(Predicate<CycleEndpoint> condition) {
		return new ArrayList<>(this.cycleEndpoints).stream() //
				.filter(t -> condition.test(t.getCycleEndpoint())) //
				.filter(this.cycleEndpoints::remove) //
				.map(CycleEndpointCountdown::getCycleEndpoint) //
				.toList();
	}

	private void handleEvent(Event event) {
		switch (event.getTopic()) {
		// TODO: Execute before TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, like modbus bridge
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {

			if (this.cycleEndpoints.isEmpty()) {
				return;
			}

			this.cycleEndpoints.forEach(CycleEndpointCountdown::decreaseCycleCount);

			while (!this.cycleEndpoints.isEmpty() //
					&& this.cycleEndpoints.peek().getCycleCount() == 0) {
				final var item = this.cycleEndpoints.poll();
				synchronized (item) {
					if (item.isRunning()) {
						this.log.info(
								"Process for " + item.cycleEndpoint + " is still running. Task is not queued twice");
						if (!this.cycleEndpoints.offer(item.resetTo(1))) {
							this.log.info("Unable to re-add " + item + " to queue again.");
						}
						continue;
					}

					item.setRunning(true);
				}
				this.pool.execute(this.createTask(item));

				if (!this.cycleEndpoints.offer(item.reset())) {
					this.log.warn("Unable to add " + item.cycleEndpoint + "!");
				}
			}
		}
		}
	}

	private Runnable createTask(CycleEndpointCountdown endpointItem) {
		return () -> {
			try {
				final var result = this.urlFetcher.fetchEndpoint(endpointItem.getCycleEndpoint().endpoint().get(),
						this.bridgeHttp.getDebugMode());
				endpointItem.getCycleEndpoint().onResult().accept(result);
			} catch (HttpError e) {
				endpointItem.getCycleEndpoint().onError().accept(e);
			} finally {
				synchronized (endpointItem) {
					endpointItem.setRunning(false);
				}
			}
		};
	}

}
