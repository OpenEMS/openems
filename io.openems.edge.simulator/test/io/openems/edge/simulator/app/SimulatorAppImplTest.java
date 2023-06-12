package io.openems.edge.simulator.app;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SimulatorAppImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorAppImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(SimulatorAppImpl.SINGLETON_SERVICE_PID) //
						.build()) //
		;
	}
}
