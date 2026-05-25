package io.openems.edge.fronius.ess.gen24.dccharger;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.fronius.ess.enums.PvString;
import io.openems.edge.fronius.ess.gen24.dccharger.FroniusGen24DcChargerImpl;

public class SmaStpSeDcChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new FroniusGen24DcChargerImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setCoreId("batteryInverter0") //
						.setPvString(PvString.ONE) //
						.build()) //
				.deactivate();
	}
}
