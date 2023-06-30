package io.openems.edge.evcs.ocpp.abl;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsOcppAblImplTest {

	private static final String COMPONENT_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsOcppAblImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setConnectorId(0) //
						.setOcppId("") //
						.setLogicalId("") //
						.setLimitId("") //
						.setMaxHwCurrent(32000) //
						.setMinHwCurrent(6000) //
						.build()) //
		;
	}

}
