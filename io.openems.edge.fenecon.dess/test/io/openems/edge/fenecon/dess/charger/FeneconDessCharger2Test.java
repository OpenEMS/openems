package io.openems.edge.fenecon.dess.charger;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.fenecon.dess.ess.FeneconDessEssImpl;
import io.openems.edge.fenecon.dess.ess.MyEssConfig;

public class FeneconDessCharger2Test {

	@Test
	public void test() throws Exception {
		var ess = new FeneconDessEssImpl();
		new ComponentTest(ess) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyEssConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.build()) //
		;

		new ComponentTest(new FeneconDessCharger2()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.activate(MyChargerConfig.create() //
						.setId("charger1") //
						.setEssId("ess0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}