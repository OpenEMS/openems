package io.openems.edge.io.shelly.shellypro3em;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;

public class IoShelly3EmImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShellyPro3EmImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(MeterType.CONSUMPTION_METERED) //
						.build()) //
		;
	}

}
