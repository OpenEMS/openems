package io.openems.edge.bridge.onewire.impl;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class BridgeOnewireImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeOnewireImpl()) //
				.activate(MyConfig.create() //
						.setId("onewire0") //
						.setPort("USB1") //
						.build()) //
		;
	}

}
