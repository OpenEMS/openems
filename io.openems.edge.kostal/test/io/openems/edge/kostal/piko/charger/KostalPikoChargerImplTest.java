package io.openems.edge.kostal.piko.charger;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.kostal.piko.core.impl.KostalPikoCoreImpl;

public class KostalPikoChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new KostalPikoChargerImpl()) //
				.addReference("setCore", new KostalPikoCoreImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("core0") //
						.build()) //
		;
	}
}
