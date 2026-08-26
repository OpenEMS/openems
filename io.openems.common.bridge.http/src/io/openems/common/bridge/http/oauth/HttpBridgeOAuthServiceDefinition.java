package io.openems.common.bridge.http.oauth;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public enum HttpBridgeOAuthServiceDefinition implements HttpBridgeServiceDefinition<HttpBridgeOAuthService> {
	INSTANCE;

	@Override
	public HttpBridgeOAuthService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeOAuthService(bridgeHttp);
	}
}
