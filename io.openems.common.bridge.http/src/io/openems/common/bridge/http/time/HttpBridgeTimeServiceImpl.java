package io.openems.common.bridge.http.time;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.utils.FunctionUtils;

public class HttpBridgeTimeServiceImpl implements HttpBridgeService, HttpBridgeTimeService {

	private static class TimeEndpointCountdown<T> {
		private final Lock lock = new ReentrantLock();
		private final HttpBridgeTimeService.TimeEndpoint<T> timeEndpoint;
		private volatile boolean shutdown = false;
		private Runnable shutdownCurrentTask = FunctionUtils::doNothing;

		public TimeEndpointCountdown(HttpBridgeTimeService.TimeEndpoint<T> timeEndpoint) {
			this.timeEndpoint = timeEndpoint;
		}

		public HttpBridgeTimeService.TimeEndpoint<T> getTimeEndpoint() {
			return this.timeEndpoint;
		}

		public boolean isShutdown() {
			this.lock.lock();
			try {
				return this.shutdown;
			} finally {
				this.lock.unlock();
			}
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
	private final Set<TimeEndpointCountdown<?>> timeEndpoints = ConcurrentHashMap.newKeySet();

	public HttpBridgeTimeServiceImpl(BridgeHttp bridgeHttp, BridgeHttpExecutor pool) {
		this.bridgeHttp = bridgeHttp;
		this.pool = pool;
	}

	@Override
	public <T> HttpBridgeTimeService.TimeEndpoint<T> subscribeTime(HttpBridgeTimeService.TimeEndpoint<T> endpoint) {
		Objects.requireNonNull(endpoint, "TimeEndpoint is not allowed to be null!");

		final var endpointCountdown = new TimeEndpointCountdown<>(endpoint);
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
	public Collection<HttpBridgeTimeService.TimeEndpoint<?>> removeTimeEndpointIf(
			Predicate<HttpBridgeTimeService.TimeEndpoint<?>> condition //
	) {
		final var removedEndpoints = new HashSet<>(this.timeEndpoints).stream() //
				.filter(t -> condition.test(t.getTimeEndpoint())) //
				.filter(this.timeEndpoints::remove) //
				.toList();

		removedEndpoints.forEach(TimeEndpointCountdown::shutdown);

		return removedEndpoints.stream() //
		.<HttpBridgeTimeService.TimeEndpoint<?>>map(TimeEndpointCountdown::getTimeEndpoint) //
				.toList();
	}

	@Override
	public void close() throws Exception {
		this.timeEndpoints.forEach(TimeEndpointCountdown::shutdown);
		this.timeEndpoints.clear();
	}

	private Runnable createTask(TimeEndpointCountdown<?> endpointCountdown) {
		return () -> this.bridgeHttp.request(endpointCountdown.getTimeEndpoint().endpoint().get())
				.whenComplete((result, e) -> {

					if (endpointCountdown.isShutdown()) {
						return;
					}

					final var error = getAsHttpError(e);

					try {
						final var nextDelay = getDelay(endpointCountdown.getTimeEndpoint(), result, error);

						switch (nextDelay) {
						case DelayTimeProvider.Delay.InfiniteDelay infiniteDelay //
							-> doNothing();
						case DelayTimeProvider.Delay.DurationDelay durationDelay -> {
							final var future = this.pool.schedule(this.createTask(endpointCountdown), durationDelay);
							endpointCountdown.setShutdownCurrentTask(() -> future.cancel(false));
						}
						}

					} catch (Exception scheduleException) {
						if (this.pool.isShutdown()) {
							return;
						}
						this.log.error("Unexpected exception during scheduling Task", scheduleException);
					}
				});
	}

	private static HttpError getAsHttpError(Throwable e) {
		if (e == null) {
			return null;
		}
		return e instanceof HttpError httpError ? httpError : new HttpError.UnknownError(e);
	}

	private static <T> DelayTimeProvider.Delay getDelay(//
			HttpBridgeTimeService.TimeEndpoint<T> endpoint, //
			HttpResponse<String> result, //
			HttpError error //
	) {
		var mappedResult = getMappedResult(result, error, endpoint);

		if (error != null) {
			return endpoint.delayTimeProvider().onErrorRunDelay(error);
		}
		return endpoint.delayTimeProvider().onSuccessRunDelay(mappedResult);
	}

	private static <T> T getMappedResult(HttpResponse<String> result, HttpError error, TimeEndpoint<T> endpoint) {
		if (error != null) {
			endpoint.onError().accept(error);
			return null;
		}

		return endpoint.onResult().apply(result);
	}

}