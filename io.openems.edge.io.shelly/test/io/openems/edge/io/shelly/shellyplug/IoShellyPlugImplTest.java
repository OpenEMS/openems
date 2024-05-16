package io.openems.edge.io.shelly.shellyplug;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

public class IoShellyPlugImplTest {

	private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShellyPlugImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPhase(SinglePhase.L1) //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}
}
