package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.ComponentTest;

public class KacoBlueplanetHybrid10GridMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10GridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}

}
