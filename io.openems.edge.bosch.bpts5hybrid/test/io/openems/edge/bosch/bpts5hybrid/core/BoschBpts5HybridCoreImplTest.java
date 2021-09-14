package io.openems.edge.bosch.bpts5hybrid.core;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BoschBpts5HybridCoreImplTest {

	private static final String CORE_ID = "core0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BoschBpts5HybridCoreImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CORE_ID) //
						.setIpaddress("127.0.0.1") //
						.setInterval(2) //
						.build()) //
		;
	}
}