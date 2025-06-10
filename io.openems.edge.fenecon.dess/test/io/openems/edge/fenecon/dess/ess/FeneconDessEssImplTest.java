package io.openems.edge.fenecon.dess.ess;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class FeneconDessEssImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new FeneconDessEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyEssConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.build()) //
		;
	}
}