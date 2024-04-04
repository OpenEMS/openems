package io.openems.edge.evcs.keba.kecontact;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.keba.kecontact.core.EvcsKebaKeContactCoreImpl;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsKebaKeContactImplTest {

	private static final String COMPONENT_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsKebaKeContactImpl()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("kebaKeContactCore", new EvcsKebaKeContactCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDebugMode(false) //
						.setIp("172.0.0.1") //
						.setMinHwCurrent(6000) //
						.setUseDisplay(false) //
						.setphaseSwitchActive(true)
						.build()); //
	}

}
