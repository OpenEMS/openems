package io.openems.edge.tesla.powerwall2.battery;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.tesla.powerwall2.core.TeslaPowerwall2CoreImpl;

public class TeslaPowerwall2BatteryImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TeslaPowerwall2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("core", new TeslaPowerwall2CoreImpl()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setCoreId("core0") //
						.setPhase(SinglePhase.L1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
