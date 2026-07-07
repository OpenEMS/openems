package io.openems.common.bridge.http.time;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public enum HttpBridgeTimeServiceDefinition implements HttpBridgeServiceDefinition<HttpBridgeTimeService> {
	INSTANCE;

	@Override
	public HttpBridgeTimeService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeTimeServiceImpl(bridgeHttp, executor, endpointFetcher);
	}

}