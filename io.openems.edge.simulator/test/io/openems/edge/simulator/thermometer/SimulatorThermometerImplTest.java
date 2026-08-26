package io.openems.edge.simulator.thermometer;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorThermometerImplTest {

	@Test
	void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorThermometerImpl()) //
				.activate(MyConfig.create() //
						.setId("thermometer0") //
						.setTemperature(20) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
