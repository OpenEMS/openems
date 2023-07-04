package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.timedata.test.DummyTimedata;

public class KacoBlueplanetHybrid10EssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String CORE_ID = "kacoCore0";
	private static final String TIMEDATA_ID = "timedata0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10EssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("timedata", new DummyTimedata(TIMEDATA_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setCoreId(CORE_ID) //
						.build()) //
		;
	}
}
