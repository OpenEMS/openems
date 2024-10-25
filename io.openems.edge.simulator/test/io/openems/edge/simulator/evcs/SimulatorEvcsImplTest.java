package io.openems.edge.simulator.evcs;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class SimulatorEvcsImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setMinHwPower(1000) //
						.setMaxHwPower(10000) //
						.build()) //
				.next(new TestCase()); //
	}
}
