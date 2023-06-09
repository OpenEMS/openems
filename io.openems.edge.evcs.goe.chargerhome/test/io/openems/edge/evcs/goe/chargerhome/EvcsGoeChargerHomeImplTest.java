package io.openems.edge.evcs.goe.chargerhome;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class EvcsGoeChargerHomeImplTest {

	private static final String COMPONENT_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsGoeChargerHomeImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("192.168.50.88") //
						.setMaxHwCurrent(32) //
						.setMinHwCurrent(6) //
						.build());
	}

}
