package io.openems.common.tariff.smard;

import java.time.Clock;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public class SmardApiDefinition implements HttpBridgeServiceDefinition<SmardApi> {
	private final SmardBiddingZone biddingZone;
	private final Clock clock;

	public SmardApiDefinition(SmardBiddingZone biddingZone, Clock clock) {
		this.biddingZone = biddingZone;
		this.clock = clock;
	}

	@Override
	public SmardApi create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor, EndpointFetcher endpointFetcher) {
		return new SmardApi(this.biddingZone, bridgeHttp, executor, this.clock);
	}
}
