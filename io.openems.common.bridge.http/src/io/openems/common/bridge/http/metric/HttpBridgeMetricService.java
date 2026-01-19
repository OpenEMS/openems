package io.openems.common.bridge.http.metric;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.EndpointFetcherEvents;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.function.Disposable;

public class HttpBridgeMetricService<T> implements HttpBridgeService {

	private final Disposable unsubscribe;

	private final Map<T, MetricGroup> metricGroups;

	public HttpBridgeMetricService(//
			final BridgeHttp bridgeHttp, //
			final Function<BridgeHttp.Endpoint, T> groupingFunction //
	) {
		this.metricGroups = new HashMap<>();
		final var requestTimer = new ConcurrentHashMap<Long, Long>();
		final var requestStartDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_START, eventData -> {
			this.metricGroups.compute(groupingFunction.apply(eventData.endpoint()), (endpoint, metricGroup) -> {
				if (metricGroup == null) {
					metricGroup = new MetricGroup();
				}
				return metricGroup.withRequestStartetCount(metricGroup.requestStartetCount() + 1);
			});

			requestTimer.put(eventData.requestId(), System.currentTimeMillis());
		});

		final var requestFinishedDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_FINISHED,
				eventData -> {
					final var startTime = requestTimer.remove(eventData.requestId());
					if (startTime == null) {
						return;
					}
					final var duration = Duration.ofMillis(System.currentTimeMillis() - startTime);

					this.metricGroups.compute(groupingFunction.apply(eventData.endpoint()), (endpoint, metricGroup) -> {
						if (metricGroup == null) {
							metricGroup = new MetricGroup();
						}
						return metricGroup.withRequestFinishedCount(metricGroup.requestFinishedCount() + 1)
								.withMaxDuration(metricGroup.maxDuration().compareTo(duration) < 0 ? duration
										: metricGroup.maxDuration())
								.withWholeDuration(metricGroup.wholeDuration().plus(duration));
					});
				});

		final var requestSuccessDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_SUCCESS,
				eventData -> {
					this.metricGroups.compute(groupingFunction.apply(eventData.endpoint()), (endpoint, metricGroup) -> {
						if (metricGroup == null) {
							metricGroup = new MetricGroup();
						}
						return metricGroup.withRequestSuccessCount(metricGroup.requestSuccessCount() + 1);
					});
				});

		final var requestFailedDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_FAILED,
				eventData -> {
					this.metricGroups.compute(groupingFunction.apply(eventData.endpoint()), (endpoint, metricGroup) -> {
						if (metricGroup == null) {
							metricGroup = new MetricGroup();
						}
						return metricGroup.withRequestFailedCount(metricGroup.requestFailedCount() + 1);
					});
				});

		this.unsubscribe = () -> {
			requestStartDisposable.dispose();
			requestFinishedDisposable.dispose();
			requestSuccessDisposable.dispose();
			requestFailedDisposable.dispose();
		};
	}

	@Override
	public void close() throws Exception {
		this.unsubscribe.dispose();
	}

	@Override
	public String toString() {
		return "HttpBridgeMetricService{"//
				+ "metricGroups=" + this.metricGroups//
				+ '}';
	}

	public Map<T, MetricGroup> getMetricGroups() {
		return this.metricGroups;
	}

}
