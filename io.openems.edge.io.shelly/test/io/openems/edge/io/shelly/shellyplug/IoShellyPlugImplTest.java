package io.openems.edge.io.shelly.shellyplug;

import static io.openems.common.types.MeterType.PRODUCTION;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofDummyBridge;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.SinglePhase;

public class IoShellyPlugImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShellyPlugImpl()) //
				.addReference("httpBridgeFactory", ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(SinglePhase.L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.build()) //
		;
	}
}
