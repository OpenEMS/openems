package io.openems.common.bridge.http.dummy;

import java.util.concurrent.CompletableFuture;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.DebugMode;

public class DummyBridgeHttp implements BridgeHttp {

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote this return future never completes for actual testing a call use
	 *           DummyBridgeHttpFactory#ofBridgeImpl
	 */
	@Override
	public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
		// NOTE: this future never completes
		return new CompletableFuture<>();
	}

	@Override
	public void setDebugMode(DebugMode debugMode) {
		// do nothing
	}

	@Override
	public DebugMode getDebugMode() {
		return DebugMode.OFF;
	}

	@Override
	public <T extends HttpBridgeService> T createService(HttpBridgeServiceDefinition<T> serviceDefinition) {
		return null;
	}

}
