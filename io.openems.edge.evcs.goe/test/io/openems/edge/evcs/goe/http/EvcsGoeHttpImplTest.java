package io.openems.edge.evcs.goe.http;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.PhaseRotation;

public class EvcsGoeHttpImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsGoeHttpImpl()) //
				.addReference("httpBridgeFactory",
						ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber()))
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.50.88") //
						.setMaxHwCurrent(32) //
						.setMinHwCurrent(6) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.build());
	}

}
