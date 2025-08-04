package io.openems.edge.evcs.goe.http;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.PhaseRotation;
import org.junit.Test;

import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofDummyBridge;

public class EvcsGoeHttpImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsGoeHttpImpl()) //
				.addReference("httpBridgeFactory", ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.50.88") //
						.setMaxHwCurrent(32) //
						.setMinHwCurrent(6) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.build());
	}

}
