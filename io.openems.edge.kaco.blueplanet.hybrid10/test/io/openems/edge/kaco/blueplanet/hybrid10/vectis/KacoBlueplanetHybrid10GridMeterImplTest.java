package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;

public class KacoBlueplanetHybrid10GridMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10GridMeterImpl()) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}

}
