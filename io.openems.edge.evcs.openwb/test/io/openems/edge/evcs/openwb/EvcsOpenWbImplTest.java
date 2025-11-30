package io.openems.edge.evcs.openwb;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvcsOpenWbImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsOpenWbImpl()) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("127.0.0.1") //
						.setPort(8443) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
