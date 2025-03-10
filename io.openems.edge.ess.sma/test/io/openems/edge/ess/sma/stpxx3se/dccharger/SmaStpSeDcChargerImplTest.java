package io.openems.edge.ess.sma.stpxx3se.dccharger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.sma.enums.PvString;

public class SmaStpSeDcChargerImplTest {
	
	private static final String CHARGER_ID = "charger0";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	
	@Test
	public void test() throws Exception {
		new ComponentTest(new SmaStpSeDcChargerImpl()) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setCoreId(BATTERY_INVERTER_ID) //
						.setPvString(PvString.ONE) //
						.build());
	}
}
