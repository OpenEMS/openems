package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KacoBlueplanetHybrid10PvInverterImplTest {

	private static final String PV_INVERTER_ID = "pvInverter0";
	private static final String CORE_ID = "kacoCore0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10PvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setCoreId(CORE_ID) //
						.build()) //
		;
	}
}
