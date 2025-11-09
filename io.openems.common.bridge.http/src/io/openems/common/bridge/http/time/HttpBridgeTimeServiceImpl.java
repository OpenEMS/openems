package io.openems.common.bridge.http.time;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.utils.FunctionUtils;

public class HttpBridgeTimeServiceImpl implements HttpBridgeService, HttpBridgeTimeService {

	public static class TimeEndpointCountdown {
		private final HttpBridgeTimeService.TimeEndpoint timeEndpoint;
		private volatile boolean running = false;
		private volatile boolean shutdown = false;
		private Runnable shutdownCurrentTask = FunctionUtils::doNothing;

		public TimeEndpointCountdown(HttpBridgeTimeService.TimeEndpoint timeEndpoint) {
			this.timeEndpoint = timeEndpoint;
		}

		public HttpBridgeTimeService.TimeEndpoint getTimeEndpoint() {
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

	private final Logger log = LoggerFactory.getLogger(HttpBridgeTimeServiceImpl.class);

	private final BridgeHttp bridgeHttp;
	private final BridgeHttpExecutor pool;
	private final EndpointFetcher endpointFetcher;
	private final Set<TimeEndpointCountdown> timeEndpoints = ConcurrentHashMap.newKeySet();

	public HttpBridgeTimeServiceImpl(BridgeHttp bridgeHttp, BridgeHttpExecutor pool, EndpointFetcher endpointFetcher) {
		this.bridgeHttp = bridgeHttp;
		this.pool = pool;
		this.endpointFetcher = endpointFetcher;
	}

	@Override
	public HttpBridgeTimeService.TimeEndpoint subscribeTime(HttpBridgeTimeService.TimeEndpoint endpoint) {
		Objects.requireNonNull(endpoint, "TimeEndpoint is not allowed to be null!");

		final var endpointCountdown = new TimeEndpointCountdown(endpoint);
		this.timeEndpoints.add(endpointCountdown);
		final var delay = endpoint.delayTimeProvider().onFirstRunDelay();

		switch (delay) {
		case DelayTimeProvider.Delay.InfiniteDelay infiniteDelay //
			-> doNothing();
		case DelayTimeProvider.Delay.DurationDelay durationDelay -> {
			final var future = this.pool.schedule(this.createTask(endpointCountdown), durationDelay);
			endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
		}
		}
		return endpoint;
	}

	@Override
	public Collection<HttpBridgeTimeService.TimeEndpoint> removeTimeEndpointIf(
			Predicate<HttpBridgeTimeService.TimeEndpoint> condition) {
		return new HashSet<>(this.timeEndpoints).stream() //
				.filter(t -> condition.test(t.getTimeEndpoint())) //
				.filter(this.timeEndpoints::remove) //
				.peek(TimeEndpointCountdown::shutdown) //
				.map(TimeEndpointCountdown::getTimeEndpoint) //
				.toList();
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
				result = this.endpointFetcher.fetchEndpoint(endpointCountdown.getTimeEndpoint().endpoint().get(),
						this.bridgeHttp.getDebugMode());
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
				final DelayTimeProvider.Delay nextDelay;
				if (error != null) {
					nextDelay = endpointCountdown.getTimeEndpoint().delayTimeProvider().onErrorRunDelay(error);
				} else {
					nextDelay = endpointCountdown.getTimeEndpoint().delayTimeProvider().onSuccessRunDelay(result);
				}

				switch (nextDelay) {
				case DelayTimeProvider.Delay.InfiniteDelay infiniteDelay //
					-> doNothing();
				case DelayTimeProvider.Delay.DurationDelay durationDelay -> {
					final var future = this.pool.schedule(this.createTask(endpointCountdown), durationDelay);
					endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
				}
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
	public void close() throws Exception {
		this.timeEndpoints.forEach(TimeEndpointCountdown::shutdown);
		this.timeEndpoints.clear();
	}
}