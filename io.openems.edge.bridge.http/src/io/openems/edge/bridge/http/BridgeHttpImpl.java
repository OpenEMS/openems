package io.openems.edge.bridge.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpExecutor;
import io.openems.edge.bridge.http.api.CycleSubscriber;
import io.openems.edge.bridge.http.api.EndpointFetcher;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.edge.common.event.EdgeEventConstants;

@Component(//
		scope = ServiceScope.PROTOTYPE //
)
public class BridgeHttpImpl implements BridgeHttp {

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

	public static class TimeEndpointCountdown {
		private final TimeEndpoint timeEndpoint;
		private volatile boolean running = false;
		private volatile boolean shutdown = false;
		private Runnable shutdownCurrentTask = FunctionUtils::doNothing;

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
			this.shutdownCurrentTask = shutdownCurrentTask == null ? FunctionUtils::doNothing : shutdownCurrentTask;
		}

		/**
		 * Shuts down the current execution of the active task.
		 */
		public void shutdown() {
			this.setShutdown(true);
			this.shutdownCurrentTask.run();
		}

	}

	private final Logger log = LoggerFactory.getLogger(BridgeHttpImpl.class);

	private final CycleSubscriber cycleSubscriber;
	private final EndpointFetcher urlFetcher;
	private final BridgeHttpExecutor pool;

	private final PriorityBlockingQueue<CycleEndpointCountdown> cycleEndpoints = new PriorityBlockingQueue<>(11,
			(e1, e2) -> e1.getCycleCount() - e2.getCycleCount());

	private final Set<TimeEndpointCountdown> timeEndpoints = ConcurrentHashMap.newKeySet();

	@Activate
	public BridgeHttpImpl(//
			@Reference final CycleSubscriber cycleSubscriber, //
			@Reference final EndpointFetcher urlFetcher, //
			@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) final BridgeHttpExecutor pool //
	) {
		super();
		this.cycleSubscriber = cycleSubscriber;
		this.urlFetcher = urlFetcher;
		this.pool = pool;

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
	public TimeEndpoint subscribeTime(TimeEndpoint endpoint) {
		Objects.requireNonNull(endpoint, "TimeEndpoint is not allowed to be null!");

		final var endpointCountdown = new TimeEndpointCountdown(endpoint);
		this.timeEndpoints.add(endpointCountdown);
		final var delay = endpoint.delayTimeProvider().onFirstRunDelay();

		// TODO change in java 21 to switch case
		if (delay instanceof Delay.DurationDelay durationDelay) {
			final var future = this.pool.schedule(this.createTask(endpointCountdown), durationDelay);
			endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
		}
		return endpoint;
	}

	@Override
	public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
		final var future = new CompletableFuture<HttpResponse<String>>();
		this.pool.execute(() -> {
			try {
				final var result = this.urlFetcher.fetchEndpoint(endpoint);
				future.complete(result);
			} catch (HttpError e) {
				future.completeExceptionally(e);
			} catch (Exception e) {
				future.completeExceptionally(new HttpError.UnknownError(e));
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
				final var result = this.urlFetcher.fetchEndpoint(endpointItem.getCycleEndpoint().endpoint().get());
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

	private Runnable createTask(TimeEndpointCountdown endpointCountdown) {
		return () -> {
			synchronized (endpointCountdown) {
				if (endpointCountdown.isShutdown()) {
					return;
				}
				endpointCountdown.setRunning(true);
			}
			HttpResponse<String> result = null;
			HttpError error = null;
			try {
				result = this.urlFetcher.fetchEndpoint(endpointCountdown.getTimeEndpoint().endpoint().get());
				endpointCountdown.getTimeEndpoint().onResult().accept(result);
			} catch (HttpError e) {
				endpointCountdown.getTimeEndpoint().onError().accept(e);
				error = e;
			} catch (Exception e) {
				error = new HttpError.UnknownError(e);
				endpointCountdown.getTimeEndpoint().onError().accept(error);
			}
			synchronized (endpointCountdown) {
				if (endpointCountdown.isShutdown()) {
					return;
				}
			}

			try {
				final Delay nextDelay;
				if (error != null) {
					nextDelay = endpointCountdown.getTimeEndpoint().delayTimeProvider().onErrorRunDelay(error);
				} else {
					nextDelay = endpointCountdown.getTimeEndpoint().delayTimeProvider().onSuccessRunDelay(result);
				}

				// TODO change in java 21 to switch case
				if (nextDelay instanceof Delay.InfiniteDelay) {
					// do not queue again
					return;
				} else if (nextDelay instanceof Delay.DurationDelay durationDelay) {
					final var future = this.pool.schedule(this.createTask(endpointCountdown), durationDelay);
					endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
				}

			} catch (Exception e) {
				if (this.pool.isShutdown()) {
					return;
				}
				this.log.error("Unexpected exception during Task", e);
			}
		};
	}

	@Override
	public Collection<TimeEndpoint> removeTimeEndpointIf(Predicate<TimeEndpoint> condition) {
		return new HashSet<>(this.timeEndpoints).stream() //
				.filter(t -> condition.test(t.getTimeEndpoint())) //
				.filter(this.timeEndpoints::remove) //
				.peek(TimeEndpointCountdown::shutdown) //
				.map(TimeEndpointCountdown::getTimeEndpoint) //
				.toList();
	}

	@Override
	public Collection<CycleEndpoint> removeCycleEndpointIf(Predicate<CycleEndpoint> condition) {
		return new ArrayList<>(this.cycleEndpoints).stream() //
				.filter(t -> condition.test(t.getCycleEndpoint())) //
				.filter(this.cycleEndpoints::remove) //
				.map(CycleEndpointCountdown::getCycleEndpoint) //
				.toList();
	}

}
