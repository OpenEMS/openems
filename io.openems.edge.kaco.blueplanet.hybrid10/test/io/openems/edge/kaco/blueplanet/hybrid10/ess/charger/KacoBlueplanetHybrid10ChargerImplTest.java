package io.openems.edge.kaco.blueplanet.hybrid10.ess.charger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KacoBlueplanetHybrid10ChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10ChargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}
}
