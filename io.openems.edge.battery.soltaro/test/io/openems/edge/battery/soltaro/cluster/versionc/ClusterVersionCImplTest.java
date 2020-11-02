package io.openems.edge.battery.soltaro.cluster.versionc;

import org.junit.Test;

import io.openems.edge.battery.soltaro.ModuleType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class ClusterVersionCImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new ClusterVersionCImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModuleType(ModuleType.MODULE_3_5_KWH) //
						.setNumberOfSlaves(0) //
						.setRack1Used(false) //
						.setRack2Used(false) //
						.setRack3Used(false) //
						.setRack4Used(false) //
						.setRack5Used(false) //
						.build()) //
		;
	}
}
