package io.openems.edge.ess.adstec.storaxe;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class EssAdstecStoraxeImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EssAdstecStoraxeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.deactivate();
	}
}
