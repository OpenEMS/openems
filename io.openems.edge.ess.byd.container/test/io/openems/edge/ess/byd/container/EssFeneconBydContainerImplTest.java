package io.openems.edge.ess.byd.container;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssFeneconBydContainerImplTest {

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssFeneconBydContainerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("modbus1", new DummyModbusBridge("modbus1")) //
				.addReference("modbus2", new DummyModbusBridge("modbus2")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setReadonly(true) //
						.setModbusId0("modbus0") //
						.setModbusId1("modbus1") //
						.setModbusId2("modbus2") //
						.build()) //
		;
	}
}
