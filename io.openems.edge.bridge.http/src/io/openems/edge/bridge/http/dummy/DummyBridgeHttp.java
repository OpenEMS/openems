package io.openems.edge.bridge.http.dummy;

import java.util.concurrent.CompletableFuture;

import io.openems.edge.bridge.http.api.BridgeHttp;

public class DummyBridgeHttp implements BridgeHttp {

	@Override
	public void subscribeCycle(CycleEndpoint endpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribeTime(TimeEndpoint endpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public CompletableFuture<String> request(Endpoint endpoint) {
		// Always return a CompletableFuture completed with an empty JSON object
		return CompletableFuture.completedFuture("{}");
	}

}
