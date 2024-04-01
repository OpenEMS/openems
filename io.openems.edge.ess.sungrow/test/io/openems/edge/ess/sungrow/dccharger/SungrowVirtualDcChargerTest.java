package io.openems.edge.ess.sungrow.dccharger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class SungrowVirtualDcChargerTest {

	private static final String COMPONENT_ID = "charger0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowVirtualDcCharger()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setCoreId("core0") //
						.build()) //
		;
	}
}
