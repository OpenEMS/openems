package io.openems.edge.meter.bgetech;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MeterBgeTechDrt428M2ImplTest {

	@Test
	public void testActivatePositive() throws Exception {
		new ComponentTest(new MeterBgeTechDrt428M2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(MeterType.GRID) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}