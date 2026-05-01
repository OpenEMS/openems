package io.openems.backend.metrics.prometheus.httpbridge;

import java.util.function.Function;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.UrlBuilder;

public class HttpBridgePrometheusMetricServiceDefinition
		implements HttpBridgeServiceDefinition<HttpBridgePrometheusMetricService> {

	/**
	 * Creates a {@link HttpBridgePrometheusMetricServiceDefinition} with the path
	 * of the url as the endpoint identifier.
	 * 
	 * @param component the tracking component
	 * @return a {@link HttpBridgePrometheusMetricServiceDefinition}
	 */
	public static HttpBridgePrometheusMetricServiceDefinition byPath(String component) {
		return new HttpBridgePrometheusMetricServiceDefinition(component,
				endpoint -> UrlBuilder.parse(endpoint.url()).path());
	}

	private final String component;
	private final Function<BridgeHttp.Endpoint, String> endpointIdentifierMapper;

	public HttpBridgePrometheusMetricServiceDefinition(//
			String component, //
			Function<BridgeHttp.Endpoint, String> endpointIdentifierMapper //
	) {
		this.component = component;
		this.endpointIdentifierMapper = endpointIdentifierMapper;
	}

	@Override
	public HttpBridgePrometheusMetricService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgePrometheusMetricService(bridgeHttp, this.component, this.endpointIdentifierMapper);
	}

}
