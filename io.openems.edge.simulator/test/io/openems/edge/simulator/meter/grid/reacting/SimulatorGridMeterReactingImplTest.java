package io.openems.edge.simulator.meter.grid.reacting;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SimulatorGridMeterReactingImplTest {

	private static final String COMPONENT_ID = "meter0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorGridMeterReactingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.build()) //
				.next(new TestCase());
	}

}
