package io.openems.edge.goodwe.charger.twostring;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.goodwe.ess.GoodWeEssImpl;

public class GoodWeChargerTwoStringImplTest {

	@SuppressWarnings("deprecation")
	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeChargerTwoStringImpl()) //
				.addReference("essOrBatteryInverter", new GoodWeEssImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("ess0") //
						.setPvPort(PvPort.PV_1) //
						.build());
	}
}
