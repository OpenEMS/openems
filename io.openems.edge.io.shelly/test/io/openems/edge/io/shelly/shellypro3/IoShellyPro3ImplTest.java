package io.openems.edge.io.shelly.shellypro3;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;

public class IoShellyPro3ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShellyPro3Impl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.build()) //
		;
	}

}