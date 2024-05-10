package io.openems.edge.bridge.http.dummy;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.bridge.http.api.BridgeHttp;

public class DummyBridgeHttp implements BridgeHttp {

	public final List<CycleEndpoint> cycleEndpoints = new ArrayList<>();
	public final List<TimeEndpoint> timeEndpoints = new ArrayList<>();

	private String nextRequestResult = null;

	@Override
	public void subscribeCycle(CycleEndpoint endpoint) {
		this.cycleEndpoints.add(endpoint);
	}

	@Override
	public void subscribeTime(TimeEndpoint endpoint) {
		this.timeEndpoints.add(endpoint);
	}

	@Override
	public CompletableFuture<String> request(Endpoint endpoint) {
		return completedFuture(this.nextRequestResult);
	}

	/**
	 * Mocks a result for all {@link CycleEndpoint}s.
	 * 
	 * @param result the mocked read result
	 */
	public void mockCycleResult(String result) {
		this.cycleEndpoints.forEach(//
				e -> e.result().accept(result));
	}

	/**
	 * Mocks a result for simple request {@link Endpoint}.
	 * 
	 * @param nextRequestResult the mocked read result
	 */
	public void mockRequestResult(String nextRequestResult) {
		this.nextRequestResult = nextRequestResult;
	}

	@Override
	public CompletableFuture<byte[]> requestRaw(Endpoint endpoint) {
		// TODO Auto-generated method stub
		return null;
	}

}
