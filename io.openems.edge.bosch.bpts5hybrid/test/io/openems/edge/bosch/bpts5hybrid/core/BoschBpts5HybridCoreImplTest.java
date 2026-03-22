package io.openems.edge.bosch.bpts5hybrid.core;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BoschBpts5HybridCoreImplTest {

	private static final String CORE_ID = "core0";

	@Test
	public void testDisabled() throws Exception {
		new ComponentTest(new BoschBpts5HybridCoreImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId(CORE_ID) //
						.setEnabled(false) //
						.setIpaddress("127.0.0.1") //
						.setInterval(2) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testEnabled() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		// Provide the initial connect response with WUI_SID
		httpTestBundle.forceNextSuccessfulResult(
				HttpResponse.ok("some html WUI_SID='ABCDEFGHIJKLMNO' more html"));

		var sut = new BoschBpts5HybridCoreImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId(CORE_ID) //
						.setEnabled(true) //
						.setIpaddress("127.0.0.1") //
						.setInterval(0) //
						.build()) //
				.next(new TestCase("Successful poll") //
						.onBeforeProcessImage(() -> {
							// Values response
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(
									"x|x|1.5kW|85|x|x|x|x|x|x|0.8kW|0.3kW|0.5kW|0.2kW|0.1kW"));
							// Battery status response
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(
									"<html><body><table><tr><td>Keine Störung</td></tr></table></body></html>"));
						})) //
				.next(new TestCase("Poll with battery error") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(
									"x|x|2.0kW|50|x|x|x|x|x|x|1.0kW|0.5kW|0.3kW|0.4kW|0.2kW"));
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(
									"<html><body><table><tr><td>Störung: Batteriefehler</td></tr></table></body></html>"));
						})) //
				.deactivate();
	}
}
