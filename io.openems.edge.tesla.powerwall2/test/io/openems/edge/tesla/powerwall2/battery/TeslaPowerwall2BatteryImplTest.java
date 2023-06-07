package io.openems.edge.tesla.powerwall2.battery;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.tesla.powerwall2.core.TeslaPowerwall2CoreImpl;

public class TeslaPowerwall2BatteryImplTest {

	private static final String COMPONENT_ID = "ess0";
	private static final String CORE_ID = "core0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new TeslaPowerwall2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("core", new TeslaPowerwall2CoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setCoreId(CORE_ID) //
						.setPhase(SinglePhase.L1) //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
