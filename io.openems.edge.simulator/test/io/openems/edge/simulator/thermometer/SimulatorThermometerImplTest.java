package io.openems.edge.simulator.thermometer;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorThermometerImplTest {

	private static final String COMPONENT_ID = "thermometer0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorThermometerImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTemperature(20) //
						.build()) //
				.next(new TestCase());
	}

}
