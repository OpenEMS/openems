package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KacoBlueplanetHybrid10PvInverterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10PvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}
}
