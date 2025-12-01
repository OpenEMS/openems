package io.openems.edge.simulator.meter.grid.reacting;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorGridMeterReactingImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorGridMeterReactingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.build()) //
				.next(new TestCase());
	}

}
