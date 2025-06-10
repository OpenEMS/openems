package io.openems.edge.evcs.goe.chargerhome;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class EvcsGoeChargerHomeImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsGoeChargerHomeImpl()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.50.88") //
						.setMaxHwCurrent(32) //
						.setMinHwCurrent(6) //
						.build());
	}

}
