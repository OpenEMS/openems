package io.openems.edge.bridge.http.cycle;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

@Component(service = HttpBridgeCycleServiceDefinition.class)
public class HttpBridgeCycleServiceDefinition implements HttpBridgeServiceDefinition<HttpBridgeCycleService> {

	private final CycleSubscriber cycleSubscriber;

	@Activate
	public HttpBridgeCycleServiceDefinition(@Reference CycleSubscriber cycleSubscriber) {
		this.cycleSubscriber = cycleSubscriber;
	}

	@Override
	public HttpBridgeCycleService create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor,
			EndpointFetcher endpointFetcher) {
		return new HttpBridgeCycleServiceImpl(bridgeHttp, this.cycleSubscriber, endpointFetcher, executor);
	}
}
