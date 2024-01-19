package io.openems.edge.simulator.evcs;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class SimulatorEvcsImplTest {

	private static final String ESS_ID = "evcs0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setMinHwPower(1000) //
						.setMaxHwPower(10000) //
						.build()) //
				.next(new TestCase()); //
	}
}
