package io.openems.common.bridge.http.metric;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.bridge.http.AsyncBridgeHttpExecutor;
import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.NetworkEndpointFetcher;
import io.openems.common.bridge.http.api.BridgeHttp;

public class HttpBridgeMetricServiceTest {

	@Test
	@Ignore
	public void sampleTest() throws Exception {
		final var bridge = new BridgeHttpImpl(new NetworkEndpointFetcher(), new AsyncBridgeHttpExecutor());
		final var metricService = bridge.createService(HttpBridgeMetricServiceDefinition.byUrl());

		for (int i = 0; i < 10; i++) {
			bridge.request(BridgeHttp.create("https://openems.io/").build()).get();
		}
		for (int i = 0; i < 2; i++) {
			bridge.request(BridgeHttp.create("http://test.de/").build()).get();
		}

		final var openemsEndpointMetrics = metricService.getMetricGroups().get("https://openems.io/");
		assertEquals(10, openemsEndpointMetrics.requestFinishedCount());

		final var testEndpointMetrics = metricService.getMetricGroups().get("http://test.de/");
		assertEquals(2, testEndpointMetrics.requestFinishedCount());
	}

}