package io.openems.edge.kostal.piko.charger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.kostal.piko.core.impl.KostalPikoCoreImpl;

public class KostalPikoChargerImplTest {

	private static final String COMPONENT_ID = "charger0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoChargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setCore", new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setCoreId("core0") //
						.build()) //
		;
	}
}
