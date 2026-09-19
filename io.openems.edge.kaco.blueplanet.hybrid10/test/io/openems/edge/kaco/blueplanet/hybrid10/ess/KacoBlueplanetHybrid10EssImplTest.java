package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.timedata.test.DummyTimedata;

public class KacoBlueplanetHybrid10EssImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10EssImpl()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setCoreId("kacoCore0") //
						.build()) //
		;
	}
}
