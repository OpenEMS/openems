package io.openems.edge.bridge.onewire.impl;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class BridgeOnewireImplTest {

	private static final String BRIDGE_ID = "onewire0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeOnewireImpl()) //
				.activate(MyConfig.create() //
						.setId(BRIDGE_ID) //
						.setPort("USB1") //
						.build()) //
		;
	}

}
