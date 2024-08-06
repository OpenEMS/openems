package io.openems.edge.bridge.http.dummy;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.HttpResponse;

public class DummyBridgeHttp implements BridgeHttp {

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote never gets executed in this class for actual testing a call use
	 *           DummyBridgeHttpFactory#ofBridgeImpl
	 */
	@Override
	public CycleEndpoint subscribeCycle(CycleEndpoint endpoint) {
		return endpoint;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @implNote never gets executed in this class for actual testing a call use
	 *           DummyBridgeHttpFactory#ofBridgeImpl
	 */
	@Override
	public TimeEndpoint subscribeTime(TimeEndpoint endpoint) {
		return endpoint;
	}

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
	public Collection<CycleEndpoint> removeCycleEndpointIf(Predicate<CycleEndpoint> condition) {
		return emptyList();
	}

	@Override
	public Collection<TimeEndpoint> removeTimeEndpointIf(Predicate<TimeEndpoint> condition) {
		return emptyList();
	}

	@Override
	public CompletableFuture<byte[]> requestRaw(Endpoint endpoint) {
		// TODO Auto-generated method stub
		return null;
	}

}
