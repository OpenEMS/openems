package io.openems.edge.io.shelly.shelly25;

import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofDummyBridge;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoShelly25ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShelly25Impl()) //
				.addReference("httpBridgeFactory", ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.build()) //
		;
	}

}