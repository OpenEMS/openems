package io.openems.edge.simulator.modbus;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorModbusImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorModbusImpl()) //
				.activate(MyConfig.create() //
						.setId("modbus0") //
						.build()) //
				.next(new TestCase());
	}

}
