package io.openems.edge.kostal.piko.ess;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.kostal.piko.core.impl.KostalPikoCoreImpl;

public class KostalPikoEssImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setCore", new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setCoreId("core0") //
						.build()) //
		;
	}

}
