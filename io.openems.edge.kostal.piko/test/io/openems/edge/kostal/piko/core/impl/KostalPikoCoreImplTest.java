package io.openems.edge.kostal.piko.core.impl;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class KostalPikoCoreImplTest {

	private static final String COMPONENT_ID = "core0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setPort(81) //
						.setUnitID(0xff) //
						.build()) //
		;
	}

}
