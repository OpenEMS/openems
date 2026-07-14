package io.openems.common.bridge.http.logging;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public record HttpBridgeLoggingServiceDefinition(//
		HttpBridgeLoggingServiceConfiguration config //
) implements HttpBridgeServiceDefinition<HttpBridgeLoggingService> {

	public HttpBridgeLoggingServiceDefinition() {
		this(HttpBridgeLoggingServiceConfiguration.DEFAULT);
	}

	@Override
	public HttpBridgeLoggingService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeLoggingService(bridgeHttp, this.config);
	}

}
