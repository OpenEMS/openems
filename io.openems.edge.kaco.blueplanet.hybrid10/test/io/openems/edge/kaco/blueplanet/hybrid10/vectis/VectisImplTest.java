package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class VectisImplTest {

	private static final String METER_ID = "meter0";
	private static final String CORE_ID = "kacoCore0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10GridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setCoreId(CORE_ID) //
						.build()) //
		;
	}

}
