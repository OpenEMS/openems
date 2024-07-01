package io.openems.edge.fenecon.dess.charger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.fenecon.dess.ess.FeneconDessEssImpl;
import io.openems.edge.fenecon.dess.ess.MyEssConfig;

public class FeneconDessCharger2Test {

	private static final String CHARGER_ID = "charger1";
	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		var ess = new FeneconDessEssImpl();
		new ComponentTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyEssConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.build()) //
		;

		new ComponentTest(new FeneconDessCharger2()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyChargerConfig.create() //
						.setId(CHARGER_ID) //
						.setEssId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.build()) //
		;
	}
}