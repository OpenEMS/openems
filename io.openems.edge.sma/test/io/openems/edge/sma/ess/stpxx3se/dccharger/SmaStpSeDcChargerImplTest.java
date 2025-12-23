package io.openems.edge.sma.ess.stpxx3se.dccharger;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.sma.ess.enums.PvString;

public class SmaStpSeDcChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SmaStpSeDcChargerImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("batteryInverter0") //
						.setPvString(PvString.ONE) //
						.build()) //
				.deactivate();
	}
}
