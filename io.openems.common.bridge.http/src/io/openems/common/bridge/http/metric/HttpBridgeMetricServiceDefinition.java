package io.openems.common.bridge.http.metric;

import java.util.function.Function;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public record HttpBridgeMetricServiceDefinition<T>(Function<BridgeHttp.Endpoint, T> groupingFunction)
		implements HttpBridgeServiceDefinition<HttpBridgeMetricService<T>> {

	/**
	 * Group metrics by URL.
	 * 
	 * @return the definition
	 */
	public static HttpBridgeMetricServiceDefinition<String> byUrl() {
		return new HttpBridgeMetricServiceDefinition<>(BridgeHttp.Endpoint::url);
	}

	@Override
	public HttpBridgeMetricService<T> create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeMetricService<>(bridgeHttp, this.groupingFunction);
	}

}
