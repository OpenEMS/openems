package io.openems.edge.bridge.http.dummy;

import java.util.concurrent.CompletableFuture;

import io.openems.edge.bridge.http.api.BridgeHttp;

public class DummyBridgeHttp implements BridgeHttp {

	@Override
	public void subscribe(Endpoint endpoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public CompletableFuture<String> request(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTimeout(int connectTimeout, int readTimeout) {
		// TODO Auto-generated method stub

	}

}
