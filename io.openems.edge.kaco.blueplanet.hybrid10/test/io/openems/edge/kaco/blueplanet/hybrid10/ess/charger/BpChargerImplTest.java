package io.openems.edge.kaco.blueplanet.hybrid10.ess.charger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BpChargerImplTest {

	private static final String CHARGER_ID = "charger0";
	private static final String CORE_ID = "kacoCore0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10ChargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setCoreId(CORE_ID) //
						.build()) //
		;
	}
}
