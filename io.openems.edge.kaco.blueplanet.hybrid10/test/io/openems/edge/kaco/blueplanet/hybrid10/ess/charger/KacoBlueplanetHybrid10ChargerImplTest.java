package io.openems.edge.kaco.blueplanet.hybrid10.ess.charger;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;

public class KacoBlueplanetHybrid10ChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10ChargerImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}
}
