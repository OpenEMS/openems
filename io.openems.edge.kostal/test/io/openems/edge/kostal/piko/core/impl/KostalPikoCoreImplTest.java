package io.openems.edge.kostal.piko.core.impl;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class KostalPikoCoreImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId("core0") //
						.setIp("127.0.0.1") //
						.setPort(81) //
						.setUnitID(0xff) //
						.build()) //
		;
	}

}
