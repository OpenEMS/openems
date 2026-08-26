package io.openems.edge.simulator.app;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class SimulatorAppImplTest {

	@Test
	void test() throws Exception {
		new ComponentTest(new SimulatorAppImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(SimulatorAppImpl.SINGLETON_SERVICE_PID) //
						.build()) //
				.deactivate();
	}
}
