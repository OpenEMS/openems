package io.openems.edge.timeofusetariff.luox;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.types.DebugMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TimeOfUseTariffLuoxImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new TimeOfUseTariffLuoxImpl()) //
				.addReference("bridgeHttpFactory",
						DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAccessToken("") //
						.setBackendOAuthClientIdentifier("") //
						.setRefreshToken("") //
						.setUseTestApi(true) //
						.setDebugMode(DebugMode.OFF) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
