package io.openems.edge.simulator.io;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorIoDigitalInputOutputImplTest {

	@Test
	void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorIoDigitalInputOutputImpl()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setNumberOfOutputs(3) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
