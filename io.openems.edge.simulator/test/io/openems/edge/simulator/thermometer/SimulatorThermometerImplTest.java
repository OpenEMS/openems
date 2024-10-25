package io.openems.edge.simulator.thermometer;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorThermometerImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorThermometerImpl()) //
				.activate(MyConfig.create() //
						.setId("thermometer0") //
						.setTemperature(20) //
						.build()) //
				.next(new TestCase());
	}

}
