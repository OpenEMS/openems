package io.openems.edge.simulator.datasource.single.direct;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class SimulatorDatasourceSingleDirectImplTest {

	private static final String COMPONENT_ID = "datasource0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorDatasourceSingleDirectImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setTimeDelta(0) //
						.setValues() //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
