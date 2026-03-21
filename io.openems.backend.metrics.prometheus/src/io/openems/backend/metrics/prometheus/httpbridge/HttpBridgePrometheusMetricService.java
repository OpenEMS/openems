package io.openems.backend.metrics.prometheus.httpbridge;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.EndpointFetcherEvents;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.function.Disposable;
import io.prometheus.metrics.core.datapoints.Timer;

public class HttpBridgePrometheusMetricService implements HttpBridgeService {

	private final Disposable close;

	public HttpBridgePrometheusMetricService(//
			BridgeHttp bridgeHttp, //
			String component, //
			Function<BridgeHttp.Endpoint, String> endpointIdentifierMapper //
	) {

		final var timers = new ConcurrentHashMap<Long, Timer>();
		final var requestStartDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_START, eventData -> {
			final var identifier = endpointIdentifierMapper.apply(eventData.endpoint());
			final var timer = PrometheusMetrics.HTTP_REQUEST.labelValues(component, identifier).startTimer();

			timers.put(eventData.requestId(), timer);
		});

		final var requestFinishedDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_FINISHED,
				eventData -> {
					final var timer = timers.remove(eventData.requestId());
					if (timer != null) {
						timer.close();
					}
				});

		final var requestSuccessDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_SUCCESS,
				eventData -> {
					final var identifier = endpointIdentifierMapper.apply(eventData.endpoint());
					final var code = eventData.result().status().code();

					PrometheusMetrics.HTTP_REQUEST_RESULT.labelValues(component, identifier, Integer.toString(code))
							.inc();

				});

		final var requestFailedDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_FAILED,
				eventData -> {
					final var identifier = endpointIdentifierMapper.apply(eventData.endpoint());
					final var code = switch (eventData.error()) {
					case HttpError.ResponseError responseError -> responseError.status.code();
					default -> -1;
					};

					PrometheusMetrics.HTTP_REQUEST_RESULT.labelValues(component, identifier, Integer.toString(code))
							.inc();

				});

		this.close = () -> {
			requestStartDisposable.dispose();
			requestFinishedDisposable.dispose();
			requestSuccessDisposable.dispose();
			requestFailedDisposable.dispose();
		};
	}

	@Override
	public void close() {
		this.close.dispose();
	}

}
