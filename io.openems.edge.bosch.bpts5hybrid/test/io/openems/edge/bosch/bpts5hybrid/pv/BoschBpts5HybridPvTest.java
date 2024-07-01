package io.openems.edge.bosch.bpts5hybrid.pv;

import org.junit.Test;

import io.openems.edge.bosch.bpts5hybrid.core.BoschBpts5HybridCoreImpl;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BoschBpts5HybridPvTest {

	private static final String CORE_ID = "core0";
	private static final String CHARGER_ID = "charger0";

	@Test
	public void test() throws Exception {
		var core = new BoschBpts5HybridCoreImpl();
		new ComponentTest(new BoschBpts5HybridCoreImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(io.openems.edge.bosch.bpts5hybrid.core.MyConfig.create() //
						.setId(CORE_ID) //
						.setEnabled(false) //
						.setIpaddress("127.0.0.1") //
						.setInterval(2) //
						.build()); //

		new ComponentTest(new BoschBpts5HybridPvImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("core", core) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setCoreId(CORE_ID) //
						.build()) //
		;
	}
}