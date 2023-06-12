package io.openems.edge.pvinverter.cluster;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class PvInverterClusterImplTest {

	private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterClusterImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPvInverterIds() //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
