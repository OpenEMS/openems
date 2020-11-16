package io.openems.edge.ess.fenecon.commercial40.charger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssDcChargerFeneconCommercial40Test {

	private static final String CHARGER_ID = "charger0";
	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		EssFeneconCommercial40Impl ess = new EssFeneconCommercial40Impl();
		new ManagedSymmetricEssTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.ess.fenecon.commercial40.MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.build());

		new ComponentTest(new EssDcChargerFeneconCommercial40()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setModbusId(MODBUS_ID) //
						.setEssId(ESS_ID) //
						.setMaxActualPower(0) //
						.build()) //
		;
	}
}
