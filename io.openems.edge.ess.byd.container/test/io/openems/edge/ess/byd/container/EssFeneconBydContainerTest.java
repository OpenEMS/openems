package io.openems.edge.ess.byd.container;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssFeneconBydContainerTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS0_ID = "modbus0";
	private static final String MODBUS1_ID = "modbus1";
	private static final String MODBUS2_ID = "modbus2";

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssFeneconBydContainerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS0_ID)) //
				.addReference("modbus1", new DummyModbusBridge(MODBUS1_ID)) //
				.addReference("modbus2", new DummyModbusBridge(MODBUS2_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setReadonly(true) //
						.setModbusId0(MODBUS0_ID) //
						.setModbusId1(MODBUS1_ID) //
						.setModbusId2(MODBUS2_ID) //
						.build()) //
		;
	}
}
