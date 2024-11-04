package io.openems.edge.io.shelly.shelly3em;

import static io.openems.common.types.MeterType.CONSUMPTION_METERED;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofDummyBridge;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoShelly3EmImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShelly3EmImpl()) //
				.addReference("httpBridgeFactory", ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(CONSUMPTION_METERED) //
						.build()) //
		;
	}

}
