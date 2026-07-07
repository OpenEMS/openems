package io.openems.edge.bosch.bpts5hybrid.pv;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bosch.bpts5hybrid.core.BoschBpts5HybridCoreImpl;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BoschBpts5HybridPvTest {

	@Test
	public void test() throws Exception {
		var core = new BoschBpts5HybridCoreImpl();
		new ComponentTest(core) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(io.openems.edge.bosch.bpts5hybrid.core.MyConfig.create() //
						.setId("core0") //
						.setEnabled(false) //
						.setIpaddress("127.0.0.1") //
						.setInterval(2) //
						.build()); //

		new ComponentTest(new BoschBpts5HybridPvImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("core", core) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("core0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
