package io.openems.edge.goodwe.charger.twostring;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.goodwe.ess.GoodWeEssImpl;

public class GoodWeChargerTwoStringImplTest {

	private static final String ESS_ID = "ess0";
	private static final String CHARGER_ID = "charger0";

	@SuppressWarnings("deprecation")
	@Test
	public void test() throws Exception {
		new ComponentTest(new GoodWeChargerTwoStringImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", new GoodWeEssImpl()) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(ESS_ID) //
						.setPvPort(PvPort.PV_1) //
						.build());
	}
}
