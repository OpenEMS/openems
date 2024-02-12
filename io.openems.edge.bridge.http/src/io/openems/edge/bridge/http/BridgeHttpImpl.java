package io.openems.edge.bridge.http;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

	public static class CycleEndpointCountdown {
		private volatile int cycleCount;
		public final CycleEndpoint cycleEndpoint;
		private volatile boolean running = false;

		public CycleEndpointCountdown(CycleEndpoint endpoint) {
			this.cycleCount = endpoint.cycle();
			this.cycleEndpoint = endpoint;
		}

		/**
		 * Resets the current cycle count to the initial cycle count.
		 * 
		 * @return this
		 */
		public CycleEndpointCountdown reset() {
			this.cycleCount = this.cycleEndpoint.cycle();
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

	public static class TimeEndpointCountdown {
		private final TimeEndpoint timeEndpoint;
		private volatile boolean running = false;
		private volatile boolean shutdown = false;
		private Runnable shutdownCurrentTask;

		public TimeEndpointCountdown(TimeEndpoint timeEndpoint) {
			this.timeEndpoint = timeEndpoint;
		}

		public TimeEndpoint getTimeEndpoint() {
			return this.timeEndpoint;
		}

		public boolean isRunning() {
			return this.running;
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

		public boolean isShutdown() {
			return this.shutdown;
		}

		public void setShutdown(boolean shutdown) {
			this.shutdown = shutdown;
		}

		public void setShutdownCurrentTask(Runnable shutdownCurrentTask) {
			this.shutdownCurrentTask = shutdownCurrentTask;
		}

		/**
		 * Shuts down the current execution of the active task.
		 */
		public void shutdown() {
			this.setShutdown(true);
			final var shutdownTask = this.shutdownCurrentTask;
			if (shutdownTask != null) {
				shutdownTask.run();
			}
		}

	}

	private final Logger log = LoggerFactory.getLogger(BridgeHttpImpl.class);

	@Reference
	private CycleSubscriber cycleSubscriber;

	@Reference
	private UrlFetcher urlFetcher;

	// TODO change to java 21 virtual threads
	// TODO: Single pool for every http worker & avoid same endpoint in that pool
	private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(0);

	private final PriorityQueue<CycleEndpointCountdown> cycleEndpoints = new PriorityQueue<>(
			(e1, e2) -> e1.getCycleCount() - e2.getCycleCount());

	private final Set<TimeEndpointCountdown> timeEndpoints = new HashSet<>();

	/**
	 * Activate method.
	 */
	@Activate
	public void activate() {
		this.cycleSubscriber.subscribe(this::handleEvent);
	}

	/**
	 * Deactivate method.
	 */
	@Deactivate
	public void deactivate() {
		this.cycleSubscriber.unsubscribe(this::handleEvent);
		this.cycleEndpoints.clear();
		this.timeEndpoints.forEach(TimeEndpointCountdown::shutdown);
		this.timeEndpoints.clear();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.pool, 0);
	}

	@Override
	public void subscribe(CycleEndpoint endpoint) {
		if (!this.cycleEndpoints.offer(new CycleEndpointCountdown(endpoint))) {
			this.log.warn("Unable to add " + endpoint + "!");
		}
	}

	@Override
	public void subscribeTime(TimeEndpoint endpoint) {
		final var endpointCountdown = new TimeEndpointCountdown(endpoint);
		this.timeEndpoints.add(endpointCountdown);
		final var delay = endpoint.delayTimeProvider().nextRun(true, true);
		final var future = this.pool.schedule(this.createTask(endpointCountdown), delay.amount(), delay.unit());
		endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
	}

	@Override
	public CompletableFuture<String> request(Endpoint endpoint) {
		final var future = new CompletableFuture<String>();
		this.pool.execute(() -> {
			try {
				final var result = this.urlFetcher.fetchEndpoint(endpoint);
				future.complete(result);
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	private void handleEvent(Event event) {
		switch (event.getTopic()) {
		// TODO: Execute before TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, like modbus bridge
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {

			if (this.cycleEndpoints.isEmpty()) {
				return;
			}

			this.cycleEndpoints.forEach(CycleEndpointCountdown::decreaseCycleCount);

			while (this.cycleEndpoints.peek().getCycleCount() == 0) {
				final var item = this.cycleEndpoints.poll();
				synchronized (item) {
					if (item.isRunning()) {
						this.log.info(
								"Process for " + item.cycleEndpoint + " is still running. Task is not queued twice");
						this.cycleEndpoints.add(item.reset());
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
				final var result = this.urlFetcher.fetchEndpoint(endpointItem.cycleEndpoint.endpoint());
				endpointItem.cycleEndpoint.result().accept(result);
			} catch (Exception e) {
				endpointItem.cycleEndpoint.onError().accept(e);
			} finally {
				synchronized (endpointItem) {
					endpointItem.setRunning(false);
				}
			}
		};
	}

	private Runnable createTask(TimeEndpointCountdown endpointCountdown) {
		return () -> {
			synchronized (endpointCountdown) {
				if (endpointCountdown.isShutdown()) {
					return;
				}
				endpointCountdown.setRunning(true);
			}
			boolean currentRunSuccessful;
			try {
				final var result = this.urlFetcher.fetchEndpoint(endpointCountdown.getTimeEndpoint().endpoint());
				endpointCountdown.getTimeEndpoint().onResult().accept(result);
				currentRunSuccessful = true;
			} catch (Exception e) {
				endpointCountdown.getTimeEndpoint().onError().accept(e);
				currentRunSuccessful = false;
			}
			synchronized (endpointCountdown) {
				if (endpointCountdown.isShutdown()) {
					return;
				}
			}

			try {
				final var nextDelay = endpointCountdown.getTimeEndpoint().delayTimeProvider().nextRun(false,
						currentRunSuccessful);
				System.out.println("\n\n SCHEDULE NEW TASK " + nextDelay + ", " + LocalDateTime.now() + " \n\n");
				final var future = this.pool.schedule(this.createTask(endpointCountdown), nextDelay.amount(),
						nextDelay.unit());
				endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
			} catch (Exception e) {
				if (this.pool.isShutdown()) {
					return;
				}
				this.log.error("Unexpected exception during Task", e);
			}
		};
	}

	public PriorityQueue<CycleEndpointCountdown> getCycleEndpoints() {
		return this.cycleEndpoints;
	}

	public Set<TimeEndpointCountdown> getTimeEndpoints() {
		return this.timeEndpoints;
	}

}
