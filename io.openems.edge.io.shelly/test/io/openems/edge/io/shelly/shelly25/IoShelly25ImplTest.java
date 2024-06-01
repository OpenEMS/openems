package io.openems.edge.io.shelly.shelly25;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;

public class IoShelly25ImplTest {

	private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShelly25Impl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.build()) //
		;
	}

}