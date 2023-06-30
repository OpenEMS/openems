package io.openems.edge.evcs.ocpp.ies.keywatt.singleccs;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsOcppIesKeywattSingleImplTest {

	private static final String COMPONENT_ID = "evcs0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsOcppIesKeywattSingleImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setConnectorId(0) //
						.setOcppId("") //
						.setDebugMode(false) //
						.setMaxHwPower(22000) //
						.setMinHwPower(2000) //
						.build()) //
		;
	}

}
