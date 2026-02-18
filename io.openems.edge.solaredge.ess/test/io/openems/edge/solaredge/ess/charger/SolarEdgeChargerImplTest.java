package io.openems.edge.solaredge.ess.charger;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.solaredge.ess.SolarEdgeEssImpl;

public class SolarEdgeChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SolarEdgeChargerImpl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());
	}
}
