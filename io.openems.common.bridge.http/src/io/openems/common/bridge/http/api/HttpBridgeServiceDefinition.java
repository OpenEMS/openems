package io.openems.common.bridge.http.api;

public interface HttpBridgeServiceDefinition<T extends HttpBridgeService> {

	/**
	 * Creates a new service instance.
	 *
	 * @param bridgeHttp      the {@link BridgeHttp} to use
	 * @param executor        the {@link BridgeHttpExecutor}
	 * @param endpointFetcher the {@link EndpointFetcher}
	 * @return the created service instance
	 */
	T create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor, EndpointFetcher endpointFetcher);

}